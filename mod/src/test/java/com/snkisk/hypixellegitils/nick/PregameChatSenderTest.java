package com.snkisk.hypixellegitils.nick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class PregameChatSenderTest {
    @Test
    public void readsOnlyOrdinaryVisiblePlayerChat() {
        assertEquals("Player_1", PregameChatSender.visibleName("[MVP+] Player_1: hello"));
        assertEquals("NoRank", PregameChatSender.visibleName("NoRank: hello"));
        assertNull(PregameChatSender.visibleName("The game starts in 1 second!"));
        assertNull(PregameChatSender.visibleName("[MVP+] Player_1 has joined (8/16)!"));
    }
}
