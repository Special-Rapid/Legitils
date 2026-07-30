# Checklist

- [x] UUID Blacklist state restores after a new coordinator/bootstrap lifetime.
- [x] World changes retain history but reset live detector timing.
- [x] Only accepted attributable alerts can trigger automatic Blacklist registration.
- [x] Invalid history is ignored without blocking startup.
- [x] History writing is atomic and bounded to 256 UUIDs.
- [x] `.l blacklist add/remove <player>` uses a current-world UUID when available, otherwise resolves the explicitly entered name through Mojang before changing the local UUID history.
- [x] `.l blacklist clear all` erases every stored entry.
- [x] `.l` and `.legitils` resolve to the same local command behavior.
- [x] Explicit manual unavailable-player edits resolve name-to-UUID asynchronously through Mojang; 404 and transport failures leave history unchanged.
- [x] Java 8 tests and bootstrap artifact verification pass.
- [x] `.l nickdetect on/off` atomically persists and immediately applies the
  separate session-only Nick setting; schema 1/2 configurations retain the
  previous enabled default until changed.
- [x] `.l status` presents Anti-cheat, Nick detection, developer self-detect,
  Blacklist, and Chat/Action Bar/Sound notification state as formatted chat lines.
- [x] `.l notify <chat|actionbar|sound> on/off` immediately persists and applies
  alert delivery settings; Sound plays the built-in GUI press once per displayed alert.
- [x] `.l dev on/off` immediately persists self-observation; self samples can
  alert but never create a Blacklist entry or WDR action.
- [ ] Manual Lunar verification: use `.l blacklist add <non-visible-player>`, wait for Mojang resolution, restart, then verify the yellow `§e⚠` suffix only appears when that UUID later becomes visible.
