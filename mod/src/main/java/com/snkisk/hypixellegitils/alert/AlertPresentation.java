package com.snkisk.hypixellegitils.alert;

import com.snkisk.hypixellegitils.config.DetectorId;
import java.util.UUID;

/** Data-only presentation consumed by the Mixin-owned Minecraft UI adapter. */
public final class AlertPresentation {
    public final long sequence;
    public final String actionBarText;
    public final String chatText;
    public final boolean alert;
    public final boolean sound;
    /** Identity is presentation metadata only; the UI adapter must revalidate it before use. */
    public final DetectorId detector;
    public final UUID playerId;

    public AlertPresentation(long sequence, String actionBarText, String chatText, boolean alert) {
        this(sequence, actionBarText, chatText, alert, false, null, null);
    }

    public AlertPresentation(long sequence, String actionBarText, String chatText, boolean alert, DetectorId detector, UUID playerId) {
        this(sequence, actionBarText, chatText, alert, false, detector, playerId);
    }

    public AlertPresentation(
        long sequence,
        String actionBarText,
        String chatText,
        boolean alert,
        boolean sound,
        DetectorId detector,
        UUID playerId
    ) {
        this.sequence = sequence;
        this.actionBarText = actionBarText;
        this.chatText = chatText;
        this.alert = alert;
        this.sound = sound;
        this.detector = detector;
        this.playerId = playerId;
    }

    /** Chat delivery is independent of whether the optional HUD alert is enabled. */
    public boolean shouldEmitChatAfter(long lastDeliveredSequence) {
        return chatText != null && sequence > lastDeliveredSequence;
    }

    /** Sound is one-shot per accepted alert sequence, unlike the temporary Action Bar state. */
    public boolean shouldEmitSoundAfter(long lastDeliveredSequence) {
        return sound && sequence > lastDeliveredSequence;
    }
}
