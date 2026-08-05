package com.snkisk.hypixellegitils.stats;

import java.util.Collections;
import java.util.List;

/** No exception or remote payload crosses the client boundary. */
public final class StatsBridgeLookupResult {
    public enum Status {
        READY,
        UNAVAILABLE,
        ALREADY_REQUESTED
    }

    public final Status status;
    public final List<StatsBridgePlayerResult> players;

    private StatsBridgeLookupResult(Status status, List<StatsBridgePlayerResult> players) {
        this.status = status;
        this.players = Collections.unmodifiableList(players);
    }

    public static StatsBridgeLookupResult unavailable() {
        return new StatsBridgeLookupResult(Status.UNAVAILABLE, Collections.<StatsBridgePlayerResult>emptyList());
    }

    public static StatsBridgeLookupResult alreadyRequested() {
        return new StatsBridgeLookupResult(Status.ALREADY_REQUESTED, Collections.<StatsBridgePlayerResult>emptyList());
    }

    public static StatsBridgeLookupResult ready(List<StatsBridgePlayerResult> players) {
        return new StatsBridgeLookupResult(Status.READY, players);
    }
}
