package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.LegitilsConfig;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bounded, Java-only owner of all released per-player detector state. */
public final class DetectionEngine {
    private static final int MAXIMUM_PLAYERS = 256;
    private static final long STALE_AFTER_MILLIS = 120000L;
    private final Map<UUID, PlayerState> states = new HashMap<UUID, PlayerState>();
    private final AutoBlockSignalCheck autoBlock = new AutoBlockSignalCheck();
    private final NoSlowSignalCheck noSlow = new NoSlowSignalCheck();
    private final KillAuraSignalCheck killAura = new KillAuraSignalCheck();
    private final LegitScaffoldSignalCheck legitScaffold = new LegitScaffoldSignalCheck();
    private final CombatDesyncSignalCheck combatDesync = new CombatDesyncSignalCheck();
    private final AirStallSignalCheck airStall = new AirStallSignalCheck();

    public DetectionEngine(LegitilsConfig config) {
    }

    public synchronized List<Evidence> observe(PlayerSample sample) {
        return observe(sample, false);
    }

    public synchronized List<Evidence> observe(PlayerSample sample, boolean bypassDetectorCooldown) {
        pruneExpired(sample.observedAtMillis);
        PlayerState state = states.get(sample.playerId);
        if (state == null) {
            if (states.size() >= MAXIMUM_PLAYERS) evictOldest();
            state = new PlayerState();
            states.put(sample.playerId, state);
        }
        state.lastObservedAtMillis = sample.observedAtMillis;
        List<Evidence> evidence = new ArrayList<Evidence>(6);
        addIfPresent(evidence, autoBlock.observe(sample, state.autoBlock));
        addIfPresent(evidence, noSlow.observe(sample, state.noSlow));
        addIfPresent(evidence, killAura.observe(sample, state.killAura));
        addIfPresent(evidence, legitScaffold.observe(sample, state.legitScaffold, bypassDetectorCooldown));
        addIfPresent(evidence, combatDesync.observe(sample, state.combatDesync));
        addIfPresent(evidence, airStall.observe(sample, state.airStall));
        return evidence;
    }

    public synchronized void pruneExpired(long nowMillis) {
        Iterator<Map.Entry<UUID, PlayerState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            PlayerState state = iterator.next().getValue();
            if (nowMillis >= state.lastObservedAtMillis && nowMillis - state.lastObservedAtMillis >= STALE_AFTER_MILLIS) iterator.remove();
        }
    }

    public synchronized void reset() {
        states.clear();
    }

    /** Clears timing patterns without forgetting detector-local cooldowns. */
    public synchronized void resetForObservationDiscontinuity() {
        for (PlayerState state : states.values()) state.resetForObservationDiscontinuity();
    }

    /** A player absent from a complete visible frame has no continuous history. */
    public synchronized void retainOnlyPlayers(java.util.Set<UUID> observedPlayers) {
        if (observedPlayers == null) {
            states.clear();
            return;
        }
        Iterator<Map.Entry<UUID, PlayerState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            if (!observedPlayers.contains(iterator.next().getKey())) iterator.remove();
        }
    }

    public synchronized int size() {
        return states.size();
    }

    private void evictOldest() {
        UUID oldest = null;
        long oldestTime = Long.MAX_VALUE;
        for (Map.Entry<UUID, PlayerState> entry : states.entrySet()) {
            if (entry.getValue().lastObservedAtMillis < oldestTime) {
                oldest = entry.getKey();
                oldestTime = entry.getValue().lastObservedAtMillis;
            }
        }
        if (oldest != null) states.remove(oldest);
    }

    private static void addIfPresent(List<Evidence> output, Evidence evidence) {
        if (evidence != null) output.add(evidence);
    }

    private static final class PlayerState {
        private long lastObservedAtMillis;
        private final AutoBlockSignalCheck.State autoBlock = new AutoBlockSignalCheck.State();
        private final NoSlowSignalCheck.State noSlow = new NoSlowSignalCheck.State();
        private final KillAuraSignalCheck.State killAura = new KillAuraSignalCheck.State();
        private final LegitScaffoldSignalCheck.State legitScaffold = new LegitScaffoldSignalCheck.State();
        private final CombatDesyncSignalCheck.State combatDesync = new CombatDesyncSignalCheck.State();
        private final AirStallSignalCheck.State airStall = new AirStallSignalCheck.State();

        private void resetForObservationDiscontinuity() {
            autoBlock.reset();
            noSlow.reset();
            killAura.reset();
            legitScaffold.resetPattern();
            combatDesync.reset();
            airStall.reset();
        }
    }
}
