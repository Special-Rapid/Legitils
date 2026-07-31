package com.snkisk.hypixellegitils.party;

/** Observes simultaneous visible Bed Wars pre-game player-count changes. */
public final class PartyScoreboardJumpDetector {
    private int previousCurrent = -1;
    private int previousMaximum = -1;

    /** Returns the signed simultaneous count change, or zero when fewer than two players changed. */
    public synchronized int observe(BedwarsPreGameState.PlayerCount playerCount) {
        if (playerCount == null || !playerCount.preGame || playerCount.current < 0 || playerCount.maximum < playerCount.current) {
            reset();
            return 0;
        }
        if (previousCurrent < 0 || previousMaximum != playerCount.maximum) {
            previousCurrent = playerCount.current;
            previousMaximum = playerCount.maximum;
            return 0;
        }
        int change = playerCount.current - previousCurrent;
        previousCurrent = playerCount.current;
        previousMaximum = playerCount.maximum;
        return Math.abs(change) >= 2 ? change : 0;
    }

    public synchronized void reset() {
        previousCurrent = -1;
        previousMaximum = -1;
    }
}
