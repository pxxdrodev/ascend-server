package com.ascend.lobby.api.bossbar;

import com.ascend.core.api.nms.NMSHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
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
        this.title = NMSHelper.color("&a&lJOGANDO NO &e&lASCEND&b&l!");

        start18UpdateTask();
    }

    public void addPlayer(Player player) {
        spawn18Wither(player);
    }

    public void removePlayer(Player player) {
        remove18Wither(player);
    }

    private void spawn18Wither(Player player) {
        if (player == null) return;
        try {
            Class<?> craftWorldClass = NMSHelper.getCraftClass("CraftWorld");
            Class<?> entityWitherClass = NMSHelper.getNMSClass("EntityWither");
            Class<?> entityClass = NMSHelper.getNMSClass("Entity");
            Class<?> worldClass = NMSHelper.getNMSClass("World");
            Class<?> packetSpawnClass = NMSHelper.getNMSClass("PacketPlayOutSpawnEntityLiving");
            Class<?> entityLivingClass = NMSHelper.getNMSClass("EntityLiving");

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

            Constructor<?> packetConstructor = packetSpawnClass.getConstructor(entityLivingClass);
            Object packet = packetConstructor.newInstance(wither);

            NMSHelper.sendPacket(player, packet);

            active18Withers.put(player.getUniqueId(), wither);
            active18EntityIds.put(player.getUniqueId(), entityId);
        } catch (Throwable t) {
            plugin.getLogger().warning("Erro ao criar BossBar 1.8: " + t.getMessage());
        }
    }

    private void remove18Wither(Player player) {
        if (player == null) return;
        Integer entityId = active18EntityIds.remove(player.getUniqueId());
        active18Withers.remove(player.getUniqueId());
        if (entityId == null) return;

        try {
            Class<?> packetDestroyClass = NMSHelper.getNMSClass("PacketPlayOutEntityDestroy");
            Constructor<?> destroyConstructor = packetDestroyClass.getConstructor(int[].class);
            Object packet = destroyConstructor.newInstance(new int[]{entityId});

            NMSHelper.sendPacket(player, packet);
        } catch (Throwable ignored) {}
    }

    private void start18UpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Object wither = active18Withers.get(player.getUniqueId());
                if (wither != null) {
                    try {
                        Class<?> entityClass = NMSHelper.getNMSClass("Entity");
                        Class<?> packetTeleportClass = NMSHelper.getNMSClass("PacketPlayOutEntityTeleport");

                        Location loc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(25));
                        entityClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class)
                                .invoke(wither, loc.getX(), loc.getY(), loc.getZ(), 0.0f, 0.0f);

                        Constructor<?> teleConstructor = packetTeleportClass.getConstructor(entityClass);
                        Object packet = teleConstructor.newInstance(wither);

                        NMSHelper.sendPacket(player, packet);
                    } catch (Throwable ignored) {}
                }
            }
        }, 20L, 20L);
    }
}
