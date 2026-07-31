package com.snkisk.hypixellegitils.nick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class BedDestructionChatSignalTest {
    @Test
    public void readsBothPersonalAndTeamBedDestructionAnnouncements() {
        assertEquals("fringelton", BedDestructionChatSignal.destroyedBy("§fBED DESTRUCTION > §fYour Bed was destroyed by §afringelton§f!"));
        assertEquals("Itzhamke99", BedDestructionChatSignal.destroyedBy("BED DESTRUCTION > Green Bed was destroyed by Itzhamke99!"));
    }

    @Test
    public void rejectsNormalChatAndMalformedActorStrings() {
        assertNull(BedDestructionChatSignal.destroyedBy("A bed has been destroyed."));
        assertNull(BedDestructionChatSignal.destroyedBy("Red Bed was destroyed by not a name!"));
    }
}
