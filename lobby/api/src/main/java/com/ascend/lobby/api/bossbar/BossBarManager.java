package com.ascend.lobby.api.bossbar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossBarManager {

    private final Plugin plugin;
    private final String title;
    private final Map<UUID, Object> active18Withers = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> active18EntityIds = new ConcurrentHashMap<>();

    public BossBarManager(Plugin plugin) {
        this.plugin = plugin;
        this.title = ChatColor.translateAlternateColorCodes('&', "&a&lJOGANDO NO &e&lASCEND&b&l!");

        start18UpdateTask();
    }

    public void addPlayer(Player player) {
        spawn18Wither(player);
    }

    public void removePlayer(Player player) {
        remove18Wither(player);
    }

    private void spawn18Wither(Player player) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit." + version + ".CraftWorld");
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Class<?> entityWitherClass = Class.forName("net.minecraft.server." + version + ".EntityWither");
            Class<?> entityClass = Class.forName("net.minecraft.server." + version + ".Entity");
            Class<?> worldClass = Class.forName("net.minecraft.server." + version + ".World");
            Class<?> packetSpawnClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutSpawnEntityLiving");

            Object nmsWorld = craftWorldClass.getMethod("getHandle").invoke(player.getWorld());
            Constructor<?> witherConstructor = entityWitherClass.getConstructor(worldClass);
            Object wither = witherConstructor.newInstance(nmsWorld);

            entityWitherClass.getMethod("setCustomName", String.class).invoke(wither, title);
            entityWitherClass.getMethod("setCustomNameVisible", boolean.class).invoke(wither, true);
            entityWitherClass.getMethod("setInvisible", boolean.class).invoke(wither, true);

            Location loc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(25));
            entityWitherClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class)
                    .invoke(wither, loc.getX(), loc.getY(), loc.getZ(), 0.0f, 0.0f);

            int entityId = (int) entityClass.getMethod("getId").invoke(wither);

            Constructor<?> packetConstructor = packetSpawnClass.getConstructor(Class.forName("net.minecraft.server." + version + ".EntityLiving"));
            Object packet = packetConstructor.newInstance(wither);

            Object craftPlayer = craftPlayerClass.cast(player);
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));

            sendPacket.invoke(connection, packet);

            active18Withers.put(player.getUniqueId(), wither);
            active18EntityIds.put(player.getUniqueId(), entityId);
        } catch (Throwable t) {
            plugin.getLogger().warning("Erro ao criar BossBar 1.8: " + t.getMessage());
        }
    }

    private void remove18Wither(Player player) {
        Integer entityId = active18EntityIds.remove(player.getUniqueId());
        active18Withers.remove(player.getUniqueId());
        if (entityId == null) return;

        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Class<?> packetDestroyClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityDestroy");

            Constructor<?> destroyConstructor = packetDestroyClass.getConstructor(int[].class);
            Object packet = destroyConstructor.newInstance(new int[]{entityId});

            Object craftPlayer = craftPlayerClass.cast(player);
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));

            sendPacket.invoke(connection, packet);
        } catch (Throwable ignored) {}
    }

    private void start18UpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Object wither = active18Withers.get(player.getUniqueId());
                if (wither != null) {
                    try {
                        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                        Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
                        Class<?> entityClass = Class.forName("net.minecraft.server." + version + ".Entity");
                        Class<?> packetTeleportClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityTeleport");

                        Location loc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(25));
                        entityClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class)
                                .invoke(wither, loc.getX(), loc.getY(), loc.getZ(), 0.0f, 0.0f);

                        Constructor<?> teleConstructor = packetTeleportClass.getConstructor(entityClass);
                        Object packet = teleConstructor.newInstance(wither);

                        Object craftPlayer = craftPlayerClass.cast(player);
                        Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
                        Object connection = handle.getClass().getField("playerConnection").get(handle);
                        Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));

                        sendPacket.invoke(connection, packet);
                    } catch (Throwable ignored) {}
                }
            }
        }, 5L, 5L);
    }
}
