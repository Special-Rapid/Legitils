package com.snkisk.hypixellegitils.alert;

import com.snkisk.hypixellegitils.BuildInfo;

/** Fixed local readiness text rendered only by the Minecraft UI adapter. */
public final class LocalNotice {
    private LocalNotice() {
    }

    public static String injectedText() {
        return ChatFormat.line("§aInjected! §8| §7Build §f" + BuildInfo.displayVersion());
    }

    /** Emits once per client process, when the first non-null world becomes available. */
    public static boolean shouldShowFor(boolean alreadyShown, Object currentWorld) {
        return currentWorld != null && !alreadyShown;
    }
}
