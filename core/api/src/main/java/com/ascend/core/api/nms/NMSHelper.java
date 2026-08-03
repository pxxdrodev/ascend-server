package com.ascend.core.api.nms;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NMSHelper {

    private static String version;
    private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    static {
        try {
            version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        } catch (Exception ignored) {
            version = "v1_8_R3";
        }
    }

    private NMSHelper() {}

    public static String getVersion() {
        return version;
    }

    public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        String key = "nms." + name;
        if (CLASS_CACHE.containsKey(key)) return CLASS_CACHE.get(key);
        Class<?> clazz = Class.forName("net.minecraft.server." + version + "." + name);
        CLASS_CACHE.put(key, clazz);
        return clazz;
    }

    public static Class<?> getCraftClass(String name) throws ClassNotFoundException {
        String key = "craft." + name;
        if (CLASS_CACHE.containsKey(key)) return CLASS_CACHE.get(key);
        Class<?> clazz = Class.forName("org.bukkit.craftbukkit." + version + "." + name);
        CLASS_CACHE.put(key, clazz);
        return clazz;
    }

    public static Object getHandle(Object craftObject) {
        if (craftObject == null) return null;
        try {
            Method getHandleMethod = craftObject.getClass().getMethod("getHandle");
            return getHandleMethod.invoke(craftObject);
        } catch (Exception e) {
            return null;
        }
    }

    public static void sendPacket(Player player, Object packet) {
        if (player == null || packet == null) return;
        try {
            Object handle = getHandle(player);
            if (handle == null) return;
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method sendMethod = connection.getClass().getMethod("sendPacket", getNMSClass("Packet"));
            sendMethod.invoke(connection, packet);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        try {
            Class<?> titleEnum = getNMSClass("PacketPlayOutTitle$EnumTitleAction");
            Class<?> chatBase = getNMSClass("IChatBaseComponent");
            Class<?> packetTitle = getNMSClass("PacketPlayOutTitle");

            Object timesPacket = packetTitle.getConstructor(int.class, int.class, int.class)
                    .newInstance(fadeIn, stay, fadeOut);
            sendPacket(player, timesPacket);

            if (title != null) {
                Object titleComponent = createChatComponent(title);
                Object titleAction = Enum.valueOf((Class<Enum>) titleEnum, "TITLE");
                Object titlePacket = packetTitle.getConstructor(titleEnum, chatBase)
                        .newInstance(titleAction, titleComponent);
                sendPacket(player, titlePacket);
            }

            if (subtitle != null) {
                Object subComponent = createChatComponent(subtitle);
                Object subAction = Enum.valueOf((Class<Enum>) titleEnum, "SUBTITLE");
                Object subPacket = packetTitle.getConstructor(titleEnum, chatBase)
                        .newInstance(subAction, subComponent);
                sendPacket(player, subPacket);
            }
        } catch (Exception ignored) {}
    }

    public static Object createChatComponent(String text) {
        if (text == null) text = "";
        try {
            Class<?> chatSerializer = getNMSClass("IChatBaseComponent$ChatSerializer");
            String json = "{\"text\":\"" + escapeJson(color(text)) + "\"}";
            return chatSerializer.getMethod("a", String.class).invoke(null, json);
        } catch (Exception e) {
            return null;
        }
    }

    public static void setDeclaredField(Object object, String fieldName, Object value) {
        if (object == null) return;
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception ignored) {}
    }

    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
