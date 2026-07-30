# Checklist

## Before implementation

- [ ] Choose threshold/range from controlled false-positive traces.
- [ ] Decide detector weighting and count-decay/reset policy.
- [ ] Confirm safe Minecraft 1.8.9/Lunar render boundaries for Tab and NameTag.

## Future verification

- [ ] Rejected or anonymous evidence does not increment any player's count.
- [ ] Below-threshold, at-threshold, stale, reset, and global-lag traces pass.
- [ ] Tab and NameTag markers are local-only and retain other formatting.
- [ ] Direct and local-proxy Lunar smoke tests pass.
