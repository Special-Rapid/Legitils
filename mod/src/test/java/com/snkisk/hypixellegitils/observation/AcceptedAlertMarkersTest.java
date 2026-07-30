package com.snkisk.hypixellegitils.observation;

import com.snkisk.hypixellegitils.config.MarkerHistoryEntry;
import com.snkisk.hypixellegitils.config.MarkerHistoryStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AcceptedAlertMarkersTest {
    @Test
    public void evictsOneEntryAtTheBoundEvenWhenEveryTimestampIsLongMaxValue() {
        AcceptedAlertMarkers markers = new AcceptedAlertMarkers();
        Map<UUID, MarkerHistoryEntry> history = new LinkedHashMap<UUID, MarkerHistoryEntry>();
        for (int index = 0; index < MarkerHistoryStore.MAXIMUM_ENTRIES; index++) {
            history.put(UUID.randomUUID(), new MarkerHistoryEntry(1, false, Long.MAX_VALUE));
        }
        markers.restore(history);
        UUID added = UUID.randomUUID();
        assertTrue(markers.blacklist(added, 1L));
        assertEquals(MarkerHistoryStore.MAXIMUM_ENTRIES, markers.snapshot().size());
        assertTrue(markers.snapshot().containsKey(added));
    }
}
