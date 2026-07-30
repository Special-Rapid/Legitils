package com.snkisk.hypixellegitils.observation;

import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PlayerObservationStoreTest {
    @Test
    public void evictsOldestAndResetsForWorldChange() {
        PlayerObservationStore store = new PlayerObservationStore(2, 100L);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        store.observe(first, 10L);
        store.observe(second, 20L);
        store.observe(third, 30L);
        assertEquals(2, store.size());
        assertNull(store.get(first));
        store.reset();
        assertEquals(0, store.size());
    }

    @Test
    public void prunesStalePlayersButKeepsRecentPlayers() {
        PlayerObservationStore store = new PlayerObservationStore(3, 100L);
        UUID stale = UUID.randomUUID();
        UUID recent = UUID.randomUUID();
        store.observe(stale, 0L);
        store.observe(recent, 90L);
        store.pruneExpired(100L);
        assertNull(store.get(stale));
        assertEquals(recent, store.get(recent).playerId);
    }
}
