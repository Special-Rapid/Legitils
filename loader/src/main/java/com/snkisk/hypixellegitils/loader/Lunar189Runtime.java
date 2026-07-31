package com.snkisk.hypixellegitils.loader;

/**
 * Fail-closed profile gate for the Lunar runtime verified by this project.
 *
 * Lunar exposes the profile-specific Ichor boot-log path before Java agents
 * run. Lunar's 1.8 profile is its 1.8.9 runtime; all other values are
 * intentionally unsupported by this MOD.
 */
final class Lunar189Runtime {
    private static final String ICHOR_LOGS_FILE_PROPERTY = "ichor.logsFile";
    private static final String LUNAR_189_LOG_SUFFIX = "/profiles/1.8/logs/ichor-boot.log";

    private Lunar189Runtime() {
    }

    static boolean isSupported() {
        return isSupportedIchorLogsFile(System.getProperty(ICHOR_LOGS_FILE_PROPERTY));
    }

    static boolean isSupportedIchorLogsFile(String logsFile) {
        if (logsFile == null) return false;
        return logsFile.replace('\\', '/').endsWith(LUNAR_189_LOG_SUFFIX);
    }
}
