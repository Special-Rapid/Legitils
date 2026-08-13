package com.snkisk.hypixellegitils;

import java.io.InputStream;
import java.util.Properties;

/** Reads the build identity embedded in the packaged MOD JAR. */
public final class BuildInfo {
    private static final String RESOURCE_PATH = "/hypixellegitils-build.properties";
    private static final String VERSION_PROPERTY = "hypixellegitils.build.version";
    private static final String REVISION_PROPERTY = "hypixellegitils.build.revision";
    private static final String FALLBACK_VERSION = "0.1.0-SNAPSHOT";
    private static final String FALLBACK_REVISION = "unknown";

    private BuildInfo() {
    }

    public static String displayVersion() {
        String injectedVersion = System.getProperty(VERSION_PROPERTY);
        String injectedRevision = System.getProperty(REVISION_PROPERTY);
        if (!isBlank(injectedVersion) || !isBlank(injectedRevision)) {
            return format(injectedVersion, injectedRevision);
        }
        Properties properties = new Properties();
        InputStream input = BuildInfo.class.getResourceAsStream(RESOURCE_PATH);
        if (input == null) return format(FALLBACK_VERSION, FALLBACK_REVISION);
        try {
            properties.load(input);
            return format(properties.getProperty("version"), properties.getProperty("revision"));
        } catch (Exception ignored) {
            return format(FALLBACK_VERSION, FALLBACK_REVISION);
        } finally {
            try {
                input.close();
            } catch (Exception ignored) {
                // The read-only metadata is optional diagnostic information.
            }
        }
    }

    static String versionPropertyName() {
        return VERSION_PROPERTY;
    }

    static String revisionPropertyName() {
        return REVISION_PROPERTY;
    }

    static String format(String version, String revision) {
        String safeVersion = isBlank(version) ? FALLBACK_VERSION : version.trim();
        String safeRevision = isBlank(revision) ? FALLBACK_REVISION : revision.trim();
        return safeVersion + " (" + safeRevision + ")";
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
