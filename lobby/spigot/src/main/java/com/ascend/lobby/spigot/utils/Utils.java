package com.ascend.lobby.spigot.utils;

public final class Utils {

    private Utils() {}

    public static String serializeLocation(org.bukkit.Location loc) {
        return com.ascend.lobby.api.utils.Utils.serializeLocation(loc);
    }

    public static org.bukkit.Location deserializeLocation(String str) {
        return com.ascend.lobby.api.utils.Utils.deserializeLocation(str);
    }
}
