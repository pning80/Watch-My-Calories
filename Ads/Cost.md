# Cost Estimates

## Gemini API Cost

**Model:** `gemini-3.1-flash-lite`
**Pricing:** $0.25 / 1M input tokens, $1.50 / 1M output tokens
**Configuration:** `medium` thinking level, `media_resolution_high`

### Per-Scan Token Breakdown

| Component | Tokens |
|-----------|--------|
| Prompt text | ~150 |
| Image (`media_resolution_high`) | ~1,120 |
| **Total input** | **~1,270** |
| Output (food JSON) | ~150 |
| Thinking tokens (`medium`) | ~500–1,500 |
| **Total output** | **~650–1,650** |

### Cost Estimates

| Metric | Estimate |
|--------|----------|
| Cost per scan | $0.0013–$0.0028 |
| Cost per 1,000 scans | $1.30–$2.80 |
| Cost per 10,000 scans | $13–$28 |

### Comparison by Thinking Level

| Configuration | Cost per 1,000 scans |
|---------------|---------------------|
| `minimal` thinking (baseline) | ~$0.30 |
| `low` thinking + `high` media | ~$1.30 |
| **`medium` thinking + `high` media (chosen)** | **~$2.00** |
| `high` thinking + `high` media | ~$5.00+ |

### Notes

- `media_resolution_high` is already the Gemini default when unspecified (1,120 tokens per image). Setting it explicitly for clarity.
- The app sends 1 photo per scan, downscaled to max 2048px, JPEG quality 0.8.
- Thinking tokens are billed as output tokens.
- Estimates are based on March 2026 pricing. Check [Gemini API pricing](https://ai.google.dev/gemini-api/docs/pricing) for current rates.
