package com.ascend.core.spigot.tab;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TabHeaderFooter {

    private static final String HEADER = "\n&b&lASCEND STUDIOS\n";
    private static final String FOOTER = "\n&eWebsite &bascendstudios.com.br\n&eLoja &bloja.ascendstudios.com.br\n&eDiscord &bdiscord.gg/ascend\n";

    public static void send(Player player) {
        String header = color(HEADER);
        String footer = color(FOOTER);

        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftPlayer     = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Class<?> packetClass     = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerListHeaderFooter");
            Class<?> chatSerializer  = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");

            Object h = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escapeJson(header) + "\"}");
            Object f = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escapeJson(footer) + "\"}");

            Object packet = packetClass.getConstructor().newInstance();
            set(packet, packetClass, "a", h);
            set(packet, packetClass, "b", f);

            Object handle = craftPlayer.getMethod("getHandle").invoke(craftPlayer.cast(player));
            Object conn   = handle.getClass().getField("playerConnection").get(handle);
            conn.getClass()
                .getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"))
                .invoke(conn, packet);

        } catch (Throwable t) {
            try {
                player.getClass().getMethod("setPlayerListHeaderFooter", String.class, String.class)
                      .invoke(player, header, footer);
            } catch (Throwable ignored) {}
        }
    }

    private static void set(Object obj, Class<?> cls, String fieldName, Object value) throws Exception {
        Field f = cls.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }
}
