package com.snkisk.hypixellegitils.alert;

import com.snkisk.hypixellegitils.config.NotificationSettings;
import com.snkisk.hypixellegitils.evidence.Evidence;

/** Local-only presentation queue. It has no Minecraft, network, or sound dependency. */
public final class LocalAlertSink implements AlertSink {
    private static final long ALERT_ACTION_BAR_MILLIS = 3000L;
    private NotificationSettings settings;
    private long sequence;
    private long activeUntilMillis;
    private Evidence activeEvidence;

    public LocalAlertSink(NotificationSettings settings) {
        this.settings = settings;
    }

    public synchronized void setNotificationSettings(NotificationSettings settings) {
        if (settings == null) throw new IllegalArgumentException("Notification settings are required");
        this.settings = settings;
    }

    @Override
    public synchronized void accept(Evidence evidence, long nowMillis) {
        activeEvidence = evidence;
        activeUntilMillis = nowMillis + ALERT_ACTION_BAR_MILLIS;
        sequence++;
    }

    @Override
    public synchronized AlertPresentation presentation(long nowMillis) {
        if (activeEvidence != null && nowMillis < activeUntilMillis) {
            FlagMessage message = FlagMessage.anonymous(activeEvidence.detector);
            return new AlertPresentation(
                sequence,
                settings.overlayEnabled ? message.actionBarText : null,
                settings.chatEnabled ? message.chatPrefixText : null,
                settings.overlayEnabled,
                settings.soundEnabled,
                activeEvidence.detector,
                activeEvidence.playerId
            );
        }
        return new AlertPresentation(sequence, null, null, false);
    }

    @Override
    public synchronized void reset() {
        activeEvidence = null;
        activeUntilMillis = 0L;
    }
}
