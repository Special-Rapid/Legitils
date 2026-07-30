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
}
