#!/usr/bin/env python3
"""
Map each NEW iOS XCUITest function (added 2026-05-30 in PR #1) to the
PARITY_LEDGER.md row it claims to cover.

Strategy (revised): test NAME is the strongest intent signal. Compare keywords
from the test name + assertion-context labels against each candidate ledger row's
text, and pick the highest-scoring row.

Outputs:
  - PARITY_TEST_MAPPING.md
  - PARITY_UNMAPPED.md
"""
import re
import pathlib
from dataclasses import dataclass, field
from typing import List, Optional

REPO = pathlib.Path("/Users/pning80.git/Workspace/GitHub/Watch-My-Calories")
LEDGER = REPO / "PARITY_LEDGER.md"
UITEST_DIR = REPO / "WatchMyCalories" / "WatchMyCaloriesUITests"

# Common, low-signal words to drop from name tokens
STOP_WORDS = {
    "test", "the", "and", "for", "from", "with", "on", "in", "at", "to", "of",
    "does", "is", "are", "exists", "appears", "shows", "tap", "tapping",
    "button", "view", "card", "row", "label", "field",
}

@dataclass
class LedgerRow:
    screen: str
    idx: int
    element: str
    type_: str
    a11y_id: Optional[str]
    trigger: str
    behavior: str

    @property
    def key(self): return f"{self.screen}#{self.idx}"

    @property
    def text(self):
        return f"{self.element} {self.behavior}".lower()

def parse_ledger() -> List[LedgerRow]:
    rows = []
    text = LEDGER.read_text()
    current_screen = None
    for line in text.splitlines():
        m = re.match(r"^## (.+?)(?:\.swift)?(?: \(.+\))?\s*$", line)
        if m and not m.group(1).startswith("Coverage"):
            current_screen = m.group(1).replace(".swift", "").strip()
            continue
        m = re.match(r"^\|\s*(\d+)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|", line)
        if m and current_screen:
            a11y_raw = m.group(4).strip().strip("`")
            rows.append(LedgerRow(
                screen=current_screen, idx=int(m.group(1)),
                element=m.group(2).strip(), type_=m.group(3).strip(),
                a11y_id=None if a11y_raw == "(none)" else a11y_raw,
                trigger=m.group(5).strip(), behavior=m.group(6).strip(),
            ))
    return rows

@dataclass
class TestFunc:
    file: str
    name: str
    body: str
    assertion_a11y_ids: List[str] = field(default_factory=list)
    assertion_labels: List[str] = field(default_factory=list)
    gestures: List[str] = field(default_factory=list)
    name_tokens: List[str] = field(default_factory=list)

def tokenize(name: str) -> List[str]:
    # split camelCase: testMultiItemMealGroupSummaryRow → ["multi","item","meal","group","summary","row"]
    s = re.sub(r"^test", "", name)
    s = re.sub(r"([A-Z]+)([A-Z][a-z])", r"\1 \2", s)
    s = re.sub(r"([a-z\d])([A-Z])", r"\1 \2", s)
    return [t for t in s.lower().replace("_", " ").split() if t not in STOP_WORDS and len(t) > 2]

def parse_tests() -> List[TestFunc]:
    tests = []
    for f in sorted(UITEST_DIR.glob("*Tests.swift")):
        text = f.read_text()
        m = re.search(r"// MARK: - Parity audit \(2026-05-30\).*?$", text, re.MULTILINE)
        if not m: continue
        body_after = text[m.start():]
        # Bound each `func test*()` by the next `^    }` at the same indent level
        # — that's how Swift closes a function. Failing to do this leaks helper
        # function bodies (e.g. `private func switchToMetric()`) into the prior test.
        func_marks = list(re.finditer(r"^    func (test\w+)\(\)", body_after, re.MULTILINE))
        for mm in func_marks:
            name = mm.group(1)
            close_match = re.search(r"^    \}\s*$", body_after[mm.end():], re.MULTILINE)
            if not close_match:
                body = body_after[mm.start():]
            else:
                body = body_after[mm.start(): mm.end() + close_match.end()]

            t = TestFunc(file=f.name, name=name, body=body, name_tokens=tokenize(name))

            # Only consider IDs/labels that appear in ASSERTION lines (XCTAssert*).
            # This excludes helper-call IDs like `app.buttons["appMenu_button"]` used in setup.
            assertion_chunks = []
            for am in re.finditer(r"XCTAssert\w+\([^;]+?\)", body, re.DOTALL):
                assertion_chunks.append(am.group(0))
            # Also capture queries in lines that come 1-2 lines BEFORE an assertion
            # (e.g., `let foo = app.buttons["x"]; XCTAssertTrue(foo...)`)
            lines = body.split("\n")
            for i_l, line in enumerate(lines):
                if "XCTAssert" in line:
                    # Take preceding 3 lines as context for any `let foo = ...` setup
                    for j in range(max(0, i_l - 3), i_l):
                        assertion_chunks.append(lines[j])

            ctx = " ".join(assertion_chunks)
            for hit in re.finditer(r'"([a-z][\w]*_[\w_]+)"', ctx):
                t.assertion_a11y_ids.append(hit.group(1))
            for hit in re.finditer(r'"([A-Z][A-Za-z][A-Za-z ]+)"', ctx):
                lbl = hit.group(1).strip()
                if lbl not in ("XCTAssertTrue", "XCTAssertFalse"):
                    t.assertion_labels.append(lbl)

            for g, pat in [
                ("tap", r"\.tap\(\)"),
                ("long-press", r"\.press\(forDuration:"),
                ("type-text", r"\.typeText\("),
                ("swipe-up", r"\.swipeUp\(\)"),
                ("swipe-down", r"\.swipeDown\(\)"),
                ("swipe-left", r"\.swipeLeft\(\)"),
                ("pull-down", r"drag"),
            ]:
                if re.search(pat, body): t.gestures.append(g)
            tests.append(t)
    return tests

FILE_TO_SCREENS = {
    # First entry per file is the PREFERRED screen — scoring bonus applies.
    # Subsequent entries are valid but not preferred.
    "DashboardTests.swift": ["DashboardView", "Components / SharedComponents", "ContentView"],
    "HistoryTests.swift": ["HistoryView", "Components / SharedComponents"],
    "SettingsTests.swift": ["SettingsView"],
    "OnboardingTests.swift": ["OnboardingView"],
    "ManualEntryTests.swift": ["ManualEntryView"],
    "EstimationReviewTests.swift": ["EstimationReviewView", "Components / SharedComponents"],
    "ScanMenuTests.swift": ["ScanMenuSheet", "ScannedMenusView", "MenuCameraView", "MenuAnalysisView"],
    "AppMenuTests.swift": ["AppMenu / Toolbar", "AboutView", "BannerAdView / NativeAdView"],
    "TabNavigationTests.swift": ["ContentView"],
    "CameraCaptureTests.swift": ["CameraView"],
}

PREFERRED_SCREEN = {f: screens[0] for f, screens in FILE_TO_SCREENS.items()}

@dataclass
class Match:
    test: TestFunc
    row: LedgerRow
    score: int
    confidence: str
    reasons: List[str]

def score_match(test: TestFunc, row: LedgerRow) -> tuple[int, List[str]]:
    score = 0
    reasons = []

    # 0) Preferred-screen bonus — heavily favor rows from the test's owning file's
    # primary screen, so generic Components rows don't out-score the actual target
    # via keyword overlap.
    if PREFERRED_SCREEN.get(test.file) == row.screen:
        score += 6
        reasons.append(f"preferred screen ({row.screen})")

    # 1) Test name keyword overlap with ledger row text (weighted: this is the strongest signal)
    row_text = row.text
    name_hits = [tok for tok in test.name_tokens if tok in row_text]
    if name_hits:
        score += 5 * len(name_hits)
        reasons.append(f"name tokens match: {name_hits}")

    # 2) Assertion a11y ID matches row's a11y ID
    if row.a11y_id and row.a11y_id in test.assertion_a11y_ids:
        score += 8
        reasons.append(f"asserts on a11y_id `{row.a11y_id}`")

    # 3) Assertion label contains element keywords
    elem_keys = [w for w in re.findall(r"[A-Za-z]+", row.element) if len(w) > 3 and w[0].isupper()]
    label_hits = [lbl for lbl in test.assertion_labels for k in elem_keys if k.lower() in lbl.lower()]
    if label_hits:
        score += 3 * min(len(label_hits), 2)
        reasons.append(f"assertion labels mention: {sorted(set(label_hits))[:3]}")

    # 4) Gesture compatibility
    trigger_l = row.trigger.lower()
    test_gests = set(test.gestures)
    gesture_compatible = (
        ("tap" in trigger_l and "tap" in test_gests) or
        ("long-press" in trigger_l and "long-press" in test_gests) or
        ("swipe" in trigger_l and any(g.startswith("swipe") for g in test_gests)) or
        ("pull" in trigger_l and "pull-down" in test_gests)
    )
    if gesture_compatible:
        score += 5
        reasons.append(f"gesture compatible ({sorted(test_gests)} ↔ '{row.trigger}')")
    elif test_gests and row.trigger.lower() not in ("none", ""):
        # Test exercises a gesture but it doesn't match the row's trigger — penalize.
        score -= 4
        reasons.append(f"gesture mismatch penalty ({sorted(test_gests)} ↔ '{row.trigger}')")
    elif not test_gests and row.trigger.lower() in ("none",):
        score += 1  # display-only test on display-only row
        reasons.append("visibility test ↔ display-only row")

    return score, reasons

def match_all(tests: List[TestFunc], rows: List[LedgerRow]) -> tuple[List[Match], List[TestFunc]]:
    matches: List[Match] = []
    unmatched: List[TestFunc] = []
    for t in tests:
        screens = FILE_TO_SCREENS.get(t.file, [])
        candidates = [r for r in rows if r.screen in screens]
        scored: List[tuple[int, LedgerRow, List[str]]] = []
        for r in candidates:
            s, why = score_match(t, r)
            if s > 0:
                scored.append((s, r, why))
        if not scored:
            unmatched.append(t)
            continue
        scored.sort(key=lambda x: (-x[0], x[1].idx))
        top_score, top_row, top_reasons = scored[0]
        # confidence threshold
        if top_score >= 13:
            conf = "HIGH"
        elif top_score >= 8:
            conf = "MEDIUM"
        else:
            conf = "LOW"
        matches.append(Match(test=t, row=top_row, score=top_score, confidence=conf, reasons=top_reasons))
    return matches, unmatched

def write_outputs(matches, unmatched, rows):
    out = REPO / "PARITY_TEST_MAPPING.md"
    by_file = {}
    for m in matches:
        by_file.setdefault(m.test.file, []).append(m)
    with out.open("w") as f:
        f.write("# Parity Audit — Test → Ledger Mapping\n\n")
        f.write("Auto-generated by `/tmp/parity-audit/map_tests_to_ledger.py`. For each new XCUITest function added in PR #1 (under `// MARK: - Parity audit (2026-05-30)` sections), this table reports the `PARITY_LEDGER.md` row it covers + the confidence of the match.\n\n")
        f.write("**Heuristic:** test-name keyword overlap with ledger row text is the primary signal, plus assertion-context (only IDs/labels inside `XCTAssert*` blocks, not setup-helper calls) and gesture compatibility.\n\n")
        f.write(f"**Summary:** {len(matches)} mapped · {len(unmatched)} unmatched · "
                f"HIGH={sum(1 for m in matches if m.confidence=='HIGH')} · "
                f"MEDIUM={sum(1 for m in matches if m.confidence=='MEDIUM')} · "
                f"LOW={sum(1 for m in matches if m.confidence=='LOW')}\n\n")
        for file, ms in sorted(by_file.items()):
            f.write(f"## {file}\n\n")
            f.write("| Confidence | Test function | Ledger row | Score | Why |\n")
            f.write("|---|---|---|---:|---|\n")
            for m in sorted(ms, key=lambda mm: (mm.confidence, mm.test.name)):
                why = "; ".join(m.reasons)
                f.write(f"| **{m.confidence}** | `{m.test.name}` | {m.row.screen} #{m.row.idx} — {m.row.element} | {m.score} | {why} |\n")
            f.write("\n")

    out2 = REPO / "PARITY_UNMAPPED.md"
    matched_row_keys = {m.row.key for m in matches}
    uncovered_rows = [r for r in rows if r.key not in matched_row_keys
                      and r.trigger.lower() not in ("none",)]
    with out2.open("w") as f:
        f.write("# Parity Audit — Unmapped Tests + Uncovered Rows\n\n")
        f.write("## Tests that didn't match any ledger row\n\n")
        if unmatched:
            for t in unmatched:
                f.write(f"- `{t.file}::{t.name}` — gestures={t.gestures}, name_tokens={t.name_tokens[:6]}, assertion_ids={t.assertion_a11y_ids[:4]}, assertion_labels={t.assertion_labels[:4]}\n")
        else:
            f.write("(none)\n")
        f.write("\n## Ledger rows still uncovered by PR #1's NEW tests\n\n")
        f.write("(Excludes display-only rows. Some may be covered by EXISTING baseline tests that PR #1 didn't add — this list is just 'not closed by PR #1's new tests'.)\n\n")
        for r in uncovered_rows:
            f.write(f"- {r.screen} #{r.idx} — {r.element} (`{r.a11y_id or '(no id)'}` / {r.trigger})\n")

if __name__ == "__main__":
    rows = parse_ledger()
    tests = parse_tests()
    matches, unmatched = match_all(tests, rows)
    write_outputs(matches, unmatched, rows)
    print(f"Parsed {len(rows)} ledger rows, {len(tests)} new tests.")
    print(f"Matched {len(matches)} ({sum(1 for m in matches if m.confidence=='HIGH')} HIGH / "
          f"{sum(1 for m in matches if m.confidence=='MEDIUM')} MEDIUM / "
          f"{sum(1 for m in matches if m.confidence=='LOW')} LOW)")
    print(f"Unmatched: {len(unmatched)}")
