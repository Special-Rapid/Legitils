package com.snkisk.hypixellegitils.alert;

import com.snkisk.hypixellegitils.config.DetectorId;
import com.snkisk.hypixellegitils.config.NotificationSettings;
import com.snkisk.hypixellegitils.evidence.Confidence;
import com.snkisk.hypixellegitils.evidence.Evidence;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalAlertSinkTest {
    @Test
    public void idlePresentationHasNoActionBarAndAlertIsTemporary() {
        LocalAlertSink sink = new LocalAlertSink(new NotificationSettings(true, true, false));
        assertNull(sink.presentation(0L).actionBarText);
        sink.accept(new Evidence(DetectorId.AUTO_BLOCK, UUID.randomUUID(), Confidence.LOW, 1L, "test anomaly"), 10L);
        AlertPresentation alert = sink.presentation(20L);
        assertTrue(alert.alert);
        assertEquals("\u00a77[\u00a7fL\u00a79e\u00a71g\u00a75i\u00a7dt\u00a7ci\u00a76l\u00a7es\u00a77] \u00a7cflagged \u00a76AutoBlock", alert.actionBarText);
        assertEquals(alert.actionBarText, alert.chatText);
        assertEquals(DetectorId.AUTO_BLOCK, alert.detector);
        assertFalse(sink.presentation(3010L).alert);
        assertNull(sink.presentation(3010L).actionBarText);
    }

    @Test
    public void disabledOverlayLeavesActionBarEmptyWhileChatCanStillNotify() {
        LocalAlertSink sink = new LocalAlertSink(new NotificationSettings(true, false, false));
        sink.accept(new Evidence(DetectorId.AUTO_BLOCK, UUID.randomUUID(), Confidence.LOW, 1L, "test anomaly"), 10L);
        AlertPresentation presentation = sink.presentation(20L);
        assertFalse(presentation.alert);
        assertNull(presentation.actionBarText);
        assertEquals("\u00a77[\u00a7fL\u00a79e\u00a71g\u00a75i\u00a7dt\u00a7ci\u00a76l\u00a7es\u00a77] \u00a7cflagged \u00a76AutoBlock", presentation.chatText);
        assertTrue(presentation.shouldEmitChatAfter(0L));
        assertFalse(presentation.shouldEmitChatAfter(presentation.sequence));
    }

    @Test
    public void soundDeliveryIsOneShotForEachAlertSequence() {
        LocalAlertSink sink = new LocalAlertSink(new NotificationSettings(false, false, true));
        sink.accept(new Evidence(DetectorId.AUTO_BLOCK, UUID.randomUUID(), Confidence.LOW, 1L, "test anomaly"), 10L);
        AlertPresentation presentation = sink.presentation(20L);
        assertTrue(presentation.shouldEmitSoundAfter(0L));
        assertFalse(presentation.shouldEmitSoundAfter(presentation.sequence));
        assertNull(presentation.chatText);
        assertNull(presentation.actionBarText);
    }

    @Test
    public void disabledSoundDoesNotRequestDelivery() {
        LocalAlertSink sink = new LocalAlertSink(new NotificationSettings(true, false, false));
        sink.accept(new Evidence(DetectorId.AUTO_BLOCK, UUID.randomUUID(), Confidence.LOW, 1L, "test anomaly"), 10L);
        assertFalse(sink.presentation(20L).shouldEmitSoundAfter(0L));
    }
}
