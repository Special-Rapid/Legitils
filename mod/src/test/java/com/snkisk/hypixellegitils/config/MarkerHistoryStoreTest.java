package com.snkisk.hypixellegitils.config;

import com.snkisk.hypixellegitils.alert.LocalAlertSink;
import com.snkisk.hypixellegitils.observation.MarkerHistoryPersistence;
import com.snkisk.hypixellegitils.observation.ObservationCoordinator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MarkerHistoryStoreTest {
    @Test
    public void atomicallyRoundTripsAUuidBlacklistEntryWithEpochTimestamp() throws Exception {
        Path directory = Files.createTempDirectory("legitils-marker-history");
        Path historyPath = directory.resolve("marker-history.json");
        UUID player = UUID.randomUUID();
        Map<UUID, MarkerHistoryEntry> source = new LinkedHashMap<UUID, MarkerHistoryEntry>();
        source.put(player, new MarkerHistoryEntry(3, true, 1774920000000L, "Notch", 1774920000000L, "Notch", 1774920001000L));
        MarkerHistoryStore store = new MarkerHistoryStore();
        store.writeAtomically(historyPath, source);
        Map<UUID, MarkerHistoryEntry> restored = store.load(historyPath);
        assertEquals(1, restored.size());
        assertEquals(3, restored.get(player).acceptedCount);
        assertTrue(restored.get(player).blacklisted);
        assertEquals(1774920000000L, restored.get(player).updatedAtEpochMillis);
        assertEquals("Notch", restored.get(player).mojangResolvedName);
        assertEquals("Notch", restored.get(player).observedServerName);
    }

    @Test
    public void malformedHistoryFailsClosedToAnEmptyLocalBlacklist() throws Exception {
        Path directory = Files.createTempDirectory("legitils-marker-history-invalid");
        Path historyPath = directory.resolve("marker-history.json");
        Files.write(historyPath, "{\"schemaVersion\":1,\"entries\":[{\"playerId\":\"not-a-uuid\"}]}".getBytes("UTF-8"));
        assertFalse(new MarkerHistoryStore().load(historyPath).size() > 0);
    }

    @Test
    public void schemaOneHistoryMigratesWithoutDroppingExistingUuidEntries() throws Exception {
        Path directory = Files.createTempDirectory("legitils-marker-history-schema-one");
        Path historyPath = directory.resolve("marker-history.json");
        UUID player = UUID.randomUUID();
        String json = "{\"schemaVersion\":1,\"entries\":[{\"playerId\":\"" + player.toString()
            + "\",\"acceptedCount\":3,\"blacklisted\":true,\"updatedAtEpochMillis\":1774920000000}]}";
        Files.write(historyPath, json.getBytes("UTF-8"));
        MarkerHistoryEntry migrated = new MarkerHistoryStore().load(historyPath).get(player);
        assertEquals(3, migrated.acceptedCount);
        assertNull(migrated.mojangResolvedName);
        assertNull(migrated.observedServerName);
    }

    @Test
    public void clearAllPersistsAnEmptyHistoryForTheNextCoordinatorLifetime() throws Exception {
        Path directory = Files.createTempDirectory("legitils-marker-history-clear");
        final Path historyPath = directory.resolve("marker-history.json");
        final MarkerHistoryStore store = new MarkerHistoryStore();
        UUID player = UUID.randomUUID();
        Map<UUID, MarkerHistoryEntry> source = new LinkedHashMap<UUID, MarkerHistoryEntry>();
        source.put(player, new MarkerHistoryEntry(3, true, 1774920000000L));
        store.writeAtomically(historyPath, source);
        LegitilsConfig config = LegitilsConfig.defaults();
        ObservationCoordinator coordinator = new ObservationCoordinator(
            config,
            new LocalAlertSink(config.notifications),
            store.load(historyPath),
            new MarkerHistoryPersistence() {
                @Override
                public void write(Map<UUID, MarkerHistoryEntry> history) throws java.io.IOException {
                    store.writeAtomically(historyPath, history);
                }
            }
        );
        assertTrue(coordinator.clearAllMarkers());
        assertEquals(0, store.load(historyPath).size());
    }
}
