package com.snkisk.hypixellegitils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BuildInfoTest {
    @Test
    public void formatsTheEmbeddedVersionAndRevision() {
        assertEquals("0.1.0-SNAPSHOT (fbc3ed1)", BuildInfo.format("0.1.0-SNAPSHOT", "fbc3ed1"));
    }

    @Test
    public void usesReadableFallbacksForMissingBuildMetadata() {
        assertEquals("0.1.0-SNAPSHOT (unknown)", BuildInfo.format(null, "  "));
    }

    @Test
    public void prefersIdentityPublishedByThePreLaunchLoader() {
        String versionProperty = BuildInfo.versionPropertyName();
        String revisionProperty = BuildInfo.revisionPropertyName();
        String priorVersion = System.getProperty(versionProperty);
        String priorRevision = System.getProperty(revisionProperty);
        try {
            System.setProperty(versionProperty, "0.1.0-SNAPSHOT");
            System.setProperty(revisionProperty, "318f567");
            assertEquals("0.1.0-SNAPSHOT (318f567)", BuildInfo.displayVersion());
        } finally {
            restoreProperty(versionProperty, priorVersion);
            restoreProperty(revisionProperty, priorRevision);
        }
    }

    private void restoreProperty(String property, String value) {
        if (value == null) System.clearProperty(property);
        else System.setProperty(property, value);
    }
}
