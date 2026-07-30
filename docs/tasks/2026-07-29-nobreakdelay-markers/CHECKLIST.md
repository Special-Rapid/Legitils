# Checklist

## Automated

- [x] Two consecutive attributable immediate remote break completions can emit NoBreakDelay evidence.
- [x] Dev mode can observe a local post-break `blockHitDelay` bypass without
  persisting a Blacklist entry or exposing WDR.
- [x] A single fast interval, missing attribution/history, and reset cannot emit it.
- [x] Only accepted attributable alerts count toward a bounded marker threshold.
- [x] Anonymous, suppressed, cooldown-rejected, and world-reset evidence cannot leave a marker.
- [x] Tab and NameTag adapters preserve the original display text without a marker.
- [x] Java 8 tests and bootstrap artifact verification pass.
- [x] Final review covers ambiguous miner attribution, schema 1-to-2 marker migration, and Tab visibility.

## Lunar manual gate

- [ ] A normal player does not receive a NoBreakDelay notification during ordinary mining.
- [ ] With `.l dev on` and NoBreakDelay enabled, normal survival mining keeps
  its five-tick delay, while a consented bypass produces one self-only alert.
- [ ] Optional Tab and NameTag `⚠` suffixes coexist with Lunar formatting and disappear after world reset.
