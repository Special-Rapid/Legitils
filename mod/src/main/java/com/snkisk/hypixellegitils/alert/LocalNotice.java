package com.snkisk.hypixellegitils.alert;

/** Fixed local readiness text rendered only by the Minecraft UI adapter. */
public final class LocalNotice {
    private LocalNotice() {
    }

    public static String injectedText() {
        return ChatFormat.line("§aInjected!");
    }

    /** Emits once per client process, when the first non-null world becomes available. */
    public static boolean shouldShowFor(boolean alreadyShown, Object currentWorld) {
        return currentWorld != null && !alreadyShown;
    }
}
