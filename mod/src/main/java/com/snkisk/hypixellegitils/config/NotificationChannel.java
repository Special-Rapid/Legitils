package com.snkisk.hypixellegitils.config;

import java.util.Locale;

/** User-selectable local alert delivery channels. */
public enum NotificationChannel {
    CHAT("Chat"),
    ACTION_BAR("Action Bar"),
    SOUND("Sound");

    private final String displayName;

    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static NotificationChannel forCommand(String value) {
        if (value == null) return null;
        String normalized = value.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        if ("chat".equals(normalized)) return CHAT;
        if ("actionbar".equals(normalized) || "overlay".equals(normalized)) return ACTION_BAR;
        if ("sound".equals(normalized)) return SOUND;
        return null;
    }
}
