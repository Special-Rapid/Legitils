package com.snkisk.hypixellegitils.alert;

/** Shared local-chat brand and continuation formatting. */
public final class ChatFormat {
    /** Exact colour sequence approved in docs/mock-up/colorcode.md. */
    public static final String PREFIX = "§7[§fL§9e§1g§5i§dt§ci§6l§es§7]";
    private static final String CONTINUATION_PREFIX = "§8  › §r";

    private ChatFormat() {
    }

    public static String line(String text) {
        return PREFIX + " §r" + text;
    }

    public static String continuation(String text) {
        return CONTINUATION_PREFIX + text;
    }
}
