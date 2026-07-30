package com.snkisk.hypixellegitils.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigPaths {
    private static final String PRODUCT_DIRECTORY = "HypixelLegitils";

    private ConfigPaths() {
    }

    public static Path applicationSupportDirectory(String userHome) {
        return Paths.get(userHome, "Library", "Application Support", PRODUCT_DIRECTORY);
    }

    public static Path configPath(String userHome) {
        return applicationSupportDirectory(userHome).resolve("config.json");
    }

    public static Path runtimeStatusPath(String userHome) {
        return applicationSupportDirectory(userHome).resolve("runtime-status.json");
    }

    public static Path markerHistoryPath(String userHome) {
        return applicationSupportDirectory(userHome).resolve("marker-history.json");
    }
}
