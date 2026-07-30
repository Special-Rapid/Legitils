package com.snkisk.hypixellegitils.evidence;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.config.LegitilsConfig;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** The only component allowed to decide whether evidence becomes a local alert. */
public final class EvidencePolicy {
    /** 256 observed players times all seven fixed detector identifiers. */
    private static final int MAX_COOLDOWN_KEYS = 256 * DetectorId.values().length;
    private final Map<AlertKey, Long> lastAlertMillis = new LinkedHashMap<AlertKey, Long>();

    public synchronized PolicyDecision evaluate(Evidence evidence, EvidencePolicyContext context, LegitilsConfig config) {
        return evaluate(evidence, context, config, false);
    }

    public synchronized PolicyDecision evaluate(Evidence evidence, EvidencePolicyContext context, LegitilsConfig config, boolean bypassCooldown) {
        if (!config.isDetectorEnabled(evidence.detector)) return PolicyDecision.suppress("detector-disabled", 0L);
        if (context.globalLag) return PolicyDecision.suppress("global-lag", 0L);
        if (context.worldTransition) return PolicyDecision.suppress("world-transition", 0L);
        if (!context.sufficientHistory) return PolicyDecision.suppress("insufficient-history", 0L);

        AlertKey key = new AlertKey(evidence.detector, evidence.playerId);
        long cooldown = evidence.detector == DetectorId.AIR_STALL
            ? config.airStallCooldownMillis
            : config.normalCooldownMillis;
        Long previous = bypassCooldown ? null : lastAlertMillis.get(key);
        if (previous != null) {
            long elapsed = context.nowMillis - previous.longValue();
            if (elapsed >= 0L && elapsed < cooldown) return PolicyDecision.suppress("cooldown", cooldown - elapsed);
        }
        if (!bypassCooldown) lastAlertMillis.put(key, Long.valueOf(context.nowMillis));
        trimExpiredAndBound(context.nowMillis, config);
        return PolicyDecision.allow();
    }

    public synchronized void reset() {
        lastAlertMillis.clear();
    }

    private void trimExpiredAndBound(long nowMillis, LegitilsConfig config) {
        Iterator<Map.Entry<AlertKey, Long>> entries = lastAlertMillis.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<AlertKey, Long> entry = entries.next();
            long cooldown = entry.getKey().detector == DetectorId.AIR_STALL
                ? config.airStallCooldownMillis
                : config.normalCooldownMillis;
            if (nowMillis - entry.getValue().longValue() >= cooldown) entries.remove();
        }
        while (lastAlertMillis.size() > MAX_COOLDOWN_KEYS) {
            Iterator<AlertKey> iterator = lastAlertMillis.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private static final class AlertKey {
        private final DetectorId detector;
        private final java.util.UUID playerId;

        AlertKey(DetectorId detector, java.util.UUID playerId) {
            this.detector = detector;
            this.playerId = playerId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof AlertKey)) return false;
            AlertKey key = (AlertKey) other;
            return detector == key.detector
                && (playerId == null ? key.playerId == null : playerId.equals(key.playerId));
        }

        @Override
        public int hashCode() {
            return 31 * detector.hashCode() + (playerId == null ? 0 : playerId.hashCode());
        }
    }
}
