package com.snkisk.hypixellegitils.alert;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalNoticeTest {
    @Test
    public void injectedNoticeUsesTheLocalLegitilsPrefix() {
        assertEquals(
            ChatFormat.line("§aInjected!"),
            LocalNotice.injectedText()
        );
    }

    @Test
    public void noticeShowsOnlyForTheFirstWorldOfTheClientProcess() {
        Object firstWorld = new Object();
        Object secondWorld = new Object();
        assertTrue(LocalNotice.shouldShowFor(false, firstWorld));
        assertFalse(LocalNotice.shouldShowFor(true, firstWorld));
        assertFalse(LocalNotice.shouldShowFor(true, null));
        assertTrue(LocalNotice.shouldShowFor(false, secondWorld));
        assertFalse(LocalNotice.shouldShowFor(true, secondWorld));
    }
}
