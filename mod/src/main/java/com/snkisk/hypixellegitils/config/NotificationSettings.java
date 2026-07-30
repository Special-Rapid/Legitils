package com.snkisk.hypixellegitils.config;

/** Supplementary local notification preferences; Action Bar is alert-only. */
public final class NotificationSettings {
    public final boolean chatEnabled;
    public final boolean overlayEnabled;
    public final boolean soundEnabled;

    public NotificationSettings(boolean chatEnabled, boolean overlayEnabled, boolean soundEnabled) {
        this.chatEnabled = chatEnabled;
        this.overlayEnabled = overlayEnabled;
        this.soundEnabled = soundEnabled;
    }
}
