package com.snkisk.hypixellegitils.party;

/** The normal detector is chat-based; scoreboard jumps are a developer-only experiment. */
public enum PartyDetectionMethod {
    CHAT("chat"),
    SCOREBOARD("scoreboard");

    private final String commandName;

    PartyDetectionMethod(String commandName) {
        this.commandName = commandName;
    }

    public String commandName() {
        return commandName;
    }

    public static PartyDetectionMethod forCommand(String value) {
        if (value == null) return null;
        for (PartyDetectionMethod method : values()) {
            if (method.commandName.equals(value)) return method;
        }
        return null;
    }
}
