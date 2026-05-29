"""
mitmproxy addon that auto-saves iOS app requests during Stage 0.3 capture.

Run with:  mitmweb --listen-port 8080 -s Backend/scripts/mitm-capture.py

For every matching request, writes two files into Backend/test/contract/ios/.intermediate/:
  - <name>.request.bin   — raw HTTP request bytes (request line + headers + body)
  - <name>.response.json — pretty-printed JSON body (if response is JSON)

After capture, run scripts/redact-fixture.sh to redact + promote to the final
fixture filenames the tests load. Files in .intermediate/ are gitignored.

Matched URLs (all on watchmycalories-backend-dev-…):
  GET  /attest/challenge        → attest-challenge
  POST /attest/verify           → attest-verify
  POST /v1beta/models/default:generateContent → gemini-generate-content
"""
import json
import os
from pathlib import Path
from mitmproxy import http

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
OUT_DIR = REPO_ROOT / "Backend" / "test" / "contract" / "ios" / ".intermediate"
OUT_DIR.mkdir(parents=True, exist_ok=True)

ROUTE_MAP = {
    ("GET",  "/attest/challenge"): "attest-challenge",
    ("POST", "/attest/verify"):    "attest-verify",
    ("POST", "/v1beta/models/default:generateContent"): "gemini-generate-content",
}

# Domain match (so we only grab dev backend, not other HTTPS traffic from the iPhone)
TARGET_HOST_SUBSTR = "watchmycalories-backend-dev"


def _route_for(flow: http.HTTPFlow) -> str | None:
    if TARGET_HOST_SUBSTR not in (flow.request.host or ""):
        return None
    return ROUTE_MAP.get((flow.request.method, flow.request.path))


def _raw_request_bytes(flow: http.HTTPFlow) -> bytes:
    """Reconstruct a faithful request line + headers + body for replay."""
    req = flow.request
    request_line = f"{req.method} {req.path} HTTP/{req.http_version.split('/')[-1] if '/' in req.http_version else '1.1'}\r\n"
    headers_block = "".join(f"{k}: {v}\r\n" for k, v in req.headers.items())
    return request_line.encode("ascii", errors="replace") + headers_block.encode("ascii", errors="replace") + b"\r\n" + (req.get_content() or b"")


def response(flow: http.HTTPFlow) -> None:
    name = _route_for(flow)
    if not name:
        return

    # Save raw request bytes
    bin_path = OUT_DIR / f"{name}.request.bin"
    bin_path.write_bytes(_raw_request_bytes(flow))

    # Save request body as JSON (if it parses) for human-readable redaction
    if flow.request.content:
        try:
            req_json = json.loads(flow.request.content)
            (OUT_DIR / f"{name}.request.json").write_text(json.dumps(req_json, indent=2) + "\n")
        except (ValueError, json.JSONDecodeError):
            pass

    # Save response body as JSON
    if flow.response and flow.response.content:
        try:
            resp_json = json.loads(flow.response.content)
            (OUT_DIR / f"{name}.response.json").write_text(json.dumps(resp_json, indent=2) + "\n")
        except (ValueError, json.JSONDecodeError):
            (OUT_DIR / f"{name}.response.raw").write_bytes(flow.response.content)

    status = flow.response.status_code if flow.response else "?"
    print(f"[mitm-capture] {name}: {flow.request.method} {flow.request.path} → {status}  (saved to {OUT_DIR})")
