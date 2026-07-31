package com.snkisk.hypixellegitils.loader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Lunar189RuntimeTest {
    @Test
    public void acceptsTheVerifiedLunar189IchorProfilePath() {
        assertEquals(true, Lunar189Runtime.isSupportedIchorLogsFile(
            "/Users/example/.lunarclient/profiles/1.8/logs/ichor-boot.log"
        ));
        assertEquals(true, Lunar189Runtime.isSupportedIchorLogsFile(
            "C:\\Users\\example\\.lunarclient\\profiles\\1.8\\logs\\ichor-boot.log"
        ));
    }

    @Test
    public void rejectsOtherProfilesAndMissingIchorMetadata() {
        assertEquals(false, Lunar189Runtime.isSupportedIchorLogsFile(
            "/Users/example/.lunarclient/profiles/26/logs/ichor-boot.log"
        ));
        assertEquals(false, Lunar189Runtime.isSupportedIchorLogsFile(null));
        assertEquals(false, Lunar189Runtime.isSupportedIchorLogsFile(
            "/Users/example/.lunarclient/profiles/1.8/logs/latest.log"
        ));
    }
}
