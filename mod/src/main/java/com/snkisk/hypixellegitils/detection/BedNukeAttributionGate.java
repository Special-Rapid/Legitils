package com.snkisk.hypixellegitils.detection;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Joins three independently observed server-visible facts before attributing a BedNuke advisory:
 * a sealed-defense anomaly, a Bed-targeted break animation, and the Bed-destruction chat actor.
 */
public final class BedNukeAttributionGate {
    private static final long MATCH_WINDOW_MILLIS = 2500L;
    private static final int MAXIMUM_PENDING = 16;
    private final List<Attempt> attempts = new ArrayList<Attempt>();
    private final List<Destruction> destructions = new ArrayList<Destruction>();
    private Evidence structuralAnomaly;

    /** Records a real remote Bed-targeting animation, never a nearby-player guess. */
    public void observeBedAttempt(UUID playerId, String serverName, boolean obstructed, long nowMillis) {
        if (playerId == null || !isValidName(serverName) || nowMillis < 0L) return;
        prune(nowMillis);
        if (attempts.size() >= MAXIMUM_PENDING) attempts.remove(0);
        attempts.add(new Attempt(playerId, normalized(serverName), obstructed, nowMillis));
    }

    /** Records the actor string supplied by Hypixel's Bed-destruction chat. */
    public void observeBedDestruction(String serverName, long nowMillis) {
        if (!isValidName(serverName) || nowMillis < 0L) return;
        prune(nowMillis);
        if (destructions.size() >= MAXIMUM_PENDING) destructions.remove(0);
        destructions.add(new Destruction(normalized(serverName), nowMillis));
    }

    /** Holds the one-shot geometry finding until its chat and break-animation evidence can be matched. */
    public void observeStructuralAnomaly(Evidence evidence, long nowMillis) {
        if (evidence == null || evidence.detector != DetectorId.BED_NUKE || nowMillis < 0L) return;
        prune(nowMillis);
        structuralAnomaly = evidence;
    }

    /** Returns attributed evidence only for a matched, physically obstructed break. */
    public Evidence evaluate(long nowMillis) {
        return evaluate(nowMillis, false);
    }

    /**
     * Development mode can verify the detector without waiting for Hypixel's destruction Chat.
     * Production still requires all three independently observed facts before identifying a player.
     */
    public Evidence evaluate(long nowMillis, boolean developmentMode) {
        prune(nowMillis);
        if (structuralAnomaly == null) return null;
        if (developmentMode) {
            for (Iterator<Attempt> attemptIterator = attempts.iterator(); attemptIterator.hasNext();) {
                Attempt attempt = attemptIterator.next();
                if (!attempt.obstructed || !closeInTime(structuralAnomaly.observedAtMillis, attempt.observedAtMillis)) continue;
                attemptIterator.remove();
                structuralAnomaly = null;
                return new Evidence(
                    DetectorId.BED_NUKE,
                    attempt.playerId,
                    Confidence.HIGH,
                    nowMillis,
                    "development-confirmed obstructed Bed break"
                );
            }
            return null;
        }
        for (Iterator<Destruction> destructionIterator = destructions.iterator(); destructionIterator.hasNext();) {
            Destruction destruction = destructionIterator.next();
            if (!closeInTime(structuralAnomaly.observedAtMillis, destruction.observedAtMillis)) continue;
            for (Iterator<Attempt> attemptIterator = attempts.iterator(); attemptIterator.hasNext();) {
                Attempt attempt = attemptIterator.next();
                if (!attempt.obstructed || !attempt.serverName.equals(destruction.serverName)
                    || !closeInTime(attempt.observedAtMillis, destruction.observedAtMillis)) continue;
                destructionIterator.remove();
                attemptIterator.remove();
                structuralAnomaly = null;
                return new Evidence(
                    DetectorId.BED_NUKE,
                    attempt.playerId,
                    Confidence.HIGH,
                    nowMillis,
                    "server-confirmed obstructed Bed break"
                );
            }
        }
        return null;
    }

    public void reset() {
        attempts.clear();
        destructions.clear();
        structuralAnomaly = null;
    }

    private void prune(long nowMillis) {
        pruneAttempts(nowMillis);
        pruneDestructions(nowMillis);
        if (structuralAnomaly != null && isExpired(structuralAnomaly.observedAtMillis, nowMillis)) structuralAnomaly = null;
    }

    private void pruneAttempts(long nowMillis) {
        for (Iterator<Attempt> iterator = attempts.iterator(); iterator.hasNext();) {
            if (isExpired(iterator.next().observedAtMillis, nowMillis)) iterator.remove();
        }
    }

    private void pruneDestructions(long nowMillis) {
        for (Iterator<Destruction> iterator = destructions.iterator(); iterator.hasNext();) {
            if (isExpired(iterator.next().observedAtMillis, nowMillis)) iterator.remove();
        }
    }

    private static boolean closeInTime(long first, long second) {
        return Math.abs(second - first) <= MATCH_WINDOW_MILLIS;
    }

    private static boolean isExpired(long observedAtMillis, long nowMillis) {
        return nowMillis >= observedAtMillis && nowMillis - observedAtMillis > MATCH_WINDOW_MILLIS;
    }

    private static boolean isValidName(String value) {
        return value != null && value.matches("[A-Za-z0-9_]{1,16}");
    }

    private static String normalized(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static final class Attempt {
        private final UUID playerId;
        private final String serverName;
        private final boolean obstructed;
        private final long observedAtMillis;

        private Attempt(UUID playerId, String serverName, boolean obstructed, long observedAtMillis) {
            this.playerId = playerId;
            this.serverName = serverName;
            this.obstructed = obstructed;
            this.observedAtMillis = observedAtMillis;
        }
    }

    private static final class Destruction {
        private final String serverName;
        private final long observedAtMillis;

        private Destruction(String serverName, long observedAtMillis) {
            this.serverName = serverName;
            this.observedAtMillis = observedAtMillis;
        }
    }
}
