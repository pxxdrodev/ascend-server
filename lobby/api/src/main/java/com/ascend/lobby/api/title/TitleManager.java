package com.ascend.lobby.api.title;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TitleManager {

    private static final List<String> SUBTITLES = List.of(
            "&eBoas vindas!",
            "&e100% open source!",
            "&eQuer um café?"
    );

    private TitleManager() {}

    public static void sendRandomWelcomeTitle(Player player) {
        String randomSubtitle = SUBTITLES.get(ThreadLocalRandom.current().nextInt(SUBTITLES.size()));
        sendTitle(player, "&b&lASCEND", randomSubtitle, 10, 40, 10);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Class<?> packetTitleClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTitle");
            Class<?> enumTitleActionClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTitle$EnumTitleAction");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");

            Object handle = craftPlayerClass.getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));

            Object timesPacket = packetTitleClass.getConstructor(int.class, int.class, int.class).newInstance(fadeIn, stay, fadeOut);
            sendPacket.invoke(connection, timesPacket);

            if (title != null) {
                Object tSerialized = chatSerializerClass.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escape(color(title)) + "\"}");
                Object tEnum = enumTitleActionClass.getField("TITLE").get(null);
                Object tPacket = packetTitleClass.getConstructor(enumTitleActionClass, Class.forName("net.minecraft.server." + version + ".IChatBaseComponent")).newInstance(tEnum, tSerialized);
                sendPacket.invoke(connection, tPacket);
            }

            if (subtitle != null) {
                Object sSerialized = chatSerializerClass.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escape(color(subtitle)) + "\"}");
                Object sEnum = enumTitleActionClass.getField("SUBTITLE").get(null);
                Object sPacket = packetTitleClass.getConstructor(enumTitleActionClass, Class.forName("net.minecraft.server." + version + ".IChatBaseComponent")).newInstance(sEnum, sSerialized);
                sendPacket.invoke(connection, sPacket);
            }
        } catch (Throwable t) {
            try {
                player.sendTitle(color(title), color(subtitle));
            } catch (Throwable ignored) {}
        }
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
