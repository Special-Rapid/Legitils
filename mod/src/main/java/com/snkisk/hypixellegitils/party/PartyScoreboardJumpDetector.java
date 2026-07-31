package com.snkisk.hypixellegitils.party;

/**
 * Developer experiment that observes only a visible Bed Wars pre-game player-count jump.
 * A jump is not treated as proof of party membership outside the explicit developer test.
 */
public final class PartyScoreboardJumpDetector {
    private int previousCurrent = -1;
    private int previousMaximum = -1;

    /** Returns the observed simultaneous arrival count, or zero when there is no qualifying jump. */
    public synchronized int observe(BedwarsPreGameState.PlayerCount playerCount) {
        if (playerCount == null || !playerCount.preGame || playerCount.current < 0 || playerCount.maximum < playerCount.current) {
            reset();
            return 0;
        }
        if (previousCurrent < 0 || previousMaximum != playerCount.maximum || playerCount.current < previousCurrent) {
            previousCurrent = playerCount.current;
            previousMaximum = playerCount.maximum;
            return 0;
        }
        int joined = playerCount.current - previousCurrent;
        previousCurrent = playerCount.current;
        previousMaximum = playerCount.maximum;
        return joined >= 2 ? joined : 0;
    }

    public synchronized void reset() {
        previousCurrent = -1;
        previousMaximum = -1;
    }
}
