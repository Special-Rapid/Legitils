package com.snkisk.hypixellegitils.nick;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NickChatSignalTest {
    @Test
    public void recognizesOnlyTheSenderImmediatelyBeforeTheFirstColon() {
        assertTrue(NickChatSignal.isMessageFrom("[MVP+] Flaming: hello", "Flaming"));
        assertTrue(NickChatSignal.isMessageFrom("Flaming: hello", "Flaming"));
        assertFalse(NickChatSignal.isMessageFrom("Someone: Flaming: hello", "Flaming"));
        assertFalse(NickChatSignal.isMessageFrom("Flaming has joined (8/16)!", "Flaming"));
        assertFalse(NickChatSignal.isMessageFrom("[MVP+] NotFlaming: hello", "Flaming"));
    }

    @Test
    public void recognizesOnlyHypixelsStartMessage() {
        assertTrue(NickChatSignal.isGameStart("The game starts in 1 second!"));
        assertFalse(NickChatSignal.isGameStart("The game starts in 2 seconds!"));
    }
}
