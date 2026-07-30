package com.snkisk.hypixellegitils.observation;

import com.snkisk.hypixellegitils.config.MarkerHistoryEntry;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/** Writes the bounded local marker history without coupling observation code to the filesystem. */
public interface MarkerHistoryPersistence {
    void write(Map<UUID, MarkerHistoryEntry> history) throws IOException;
}
