package com.snkisk.hypixellegitils.nick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class PregameChatSenderTest {
    @Test
    public void readsOnlyOrdinaryVisiblePlayerChat() {
        assertEquals("Player_1", PregameChatSender.visibleName("[MVP+] Player_1: hello"));
        assertEquals("NoRank", PregameChatSender.visibleName("NoRank: hello"));
        assertEquals("SL__bz", PregameChatSender.visibleName("\u00a77SL__bz\u00a77: f"));
        assertEquals("nduasgduavdua", PregameChatSender.visibleName("\u00a7a[VIP] nduasgduavdua\u00a7f: gL"));
        assertNull(PregameChatSender.visibleName("The game starts in 1 second!"));
        assertNull(PregameChatSender.visibleName("[MVP+] Player_1 has joined (8/16)!"));
    }
}
