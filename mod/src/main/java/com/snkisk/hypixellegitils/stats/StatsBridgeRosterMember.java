package com.snkisk.hypixellegitils.stats;

import java.util.regex.Pattern;

/** Visible Minecraft identity only. A Nick remains its current display name and is never resolved here. */
public final class StatsBridgeRosterMember {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern UUID = Pattern.compile("[0-9a-fA-F-]{32,36}");

    public final String name;
    public final String uuid;

    public StatsBridgeRosterMember(String name, String uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public boolean isValid() {
        return name != null && NAME.matcher(name).matches()
            && (uuid == null || UUID.matcher(uuid).matches());
    }
}
