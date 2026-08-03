package com.ascend.lobby.spigot.npc;

import com.ascend.core.api.game.ServerType;
import com.ascend.lobby.spigot.LobbyPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

@Getter
@Setter
public class CoreNPC {

    private final String id;
    private final ServerType serverType;
    private final String serverName;
    private final Location location;
    private final String skinName;
    private List<String> hologramLines = new ArrayList<>();
    private final List<ArmorStand> hologramStands = new ArrayList<>();
    private ArmorStand interactStand;

    private Object nmsEntityPlayer;
    private int entityId;
    private UUID npcUuid;

    public CoreNPC(String id, ServerType serverType, String serverName, Location location, String skinName) {
        this.id = id;
        this.serverType = serverType;
        this.serverName = serverName;
        this.location = location;
        this.skinName = skinName != null && !skinName.trim().isEmpty() ? skinName : "Steve";
        this.npcUuid = UUID.randomUUID();

        SkinFetcher.fetchSkinAsync(this.skinName);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void spawnFor(Player player) {
        if (location == null || location.getWorld() == null) return;

        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftServerClass = Class.forName("org.bukkit.craftbukkit." + version + ".CraftServer");
            Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit." + version + ".CraftWorld");
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            Class<?> minecraftServerClass = Class.forName("net.minecraft.server." + version + ".MinecraftServer");
            Class<?> worldClass = Class.forName("net.minecraft.server." + version + ".World");
            Class<?> worldServerClass = Class.forName("net.minecraft.server." + version + ".WorldServer");
            Class<?> playerInteractClass = Class.forName("net.minecraft.server." + version + ".PlayerInteractManager");
            Class<?> packetInfoClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo");
            Class<?> enumInfoActionClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
            Class<?> packetSpawnClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutNamedEntitySpawn");
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> dataWatcherClass = Class.forName("net.minecraft.server." + version + ".DataWatcher");
            Class<?> packetMetadataClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityMetadata");
            Class<?> packetHeadRotationClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutEntityHeadRotation");

            Object nmsServer = craftServerClass.getMethod("getServer").invoke(Bukkit.getServer());
            Object nmsWorld = craftWorldClass.getMethod("getHandle").invoke(location.getWorld());

            Constructor<?> profileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
            Object profile = profileConstructor.newInstance(npcUuid, " ");

            applySkin(profile, skinName);

            Constructor<?> interactConstructor = playerInteractClass.getConstructor(worldClass);
            Object interactManager = interactConstructor.newInstance(nmsWorld);

            Constructor<?> entityConstructor = entityPlayerClass.getConstructor(minecraftServerClass, worldServerClass, gameProfileClass, playerInteractClass);
            nmsEntityPlayer = entityConstructor.newInstance(nmsServer, nmsWorld, profile, interactManager);

            Method setLocMethod = entityPlayerClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            setLocMethod.invoke(nmsEntityPlayer, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            this.entityId = (int) entityPlayerClass.getMethod("getId").invoke(nmsEntityPlayer);

            Object addAction = Enum.valueOf((Class<Enum>) enumInfoActionClass, "ADD_PLAYER");
            Constructor<?> infoConstructor = packetInfoClass.getConstructor(enumInfoActionClass, Iterable.class);
            Object infoPacket = infoConstructor.newInstance(addAction, Collections.singletonList(nmsEntityPlayer));

            Constructor<?> spawnConstructor = packetSpawnClass.getConstructor(Class.forName("net.minecraft.server." + version + ".EntityHuman"));
            Object spawnPacket = spawnConstructor.newInstance(nmsEntityPlayer);

            Object watcher = entityPlayerClass.getMethod("getDataWatcher").invoke(nmsEntityPlayer);
            dataWatcherClass.getMethod("watch", int.class, Object.class).invoke(watcher, 10, (byte) 127);

            Constructor<?> metaConstructor = packetMetadataClass.getConstructor(int.class, dataWatcherClass, boolean.class);
            Object metaPacket = metaConstructor.newInstance(this.entityId, watcher, true);

            byte yawByte = (byte) (location.getYaw() * 256.0F / 360.0F);
            Constructor<?> headRotConstructor = packetHeadRotationClass.getConstructor(Class.forName("net.minecraft.server." + version + ".Entity"), byte.class);
            Object headRotPacket = headRotConstructor.newInstance(nmsEntityPlayer, yawByte);

            Object craftPlayer = craftPlayerClass.cast(player);
            Object handle = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Method sendPacket = connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet"));

            sendPacket.invoke(connection, infoPacket);
            sendPacket.invoke(connection, spawnPacket);
            sendPacket.invoke(connection, metaPacket);
            sendPacket.invoke(connection, headRotPacket);

            Bukkit.getScheduler().runTaskLater(LobbyPlugin.getInstance(), () -> {
                try {
                    Object removeAction = Enum.valueOf((Class<Enum>) enumInfoActionClass, "REMOVE_PLAYER");
                    Object removePacket = infoConstructor.newInstance(removeAction, Collections.singletonList(nmsEntityPlayer));
                    sendPacket.invoke(connection, removePacket);
                } catch (Exception ignored) {}
            }, 40L);

        } catch (Throwable t) {
            Bukkit.getLogger().warning("Erro ao gerar Player NPC para " + id + ": " + t.getMessage());
        }
    }

    public void spawnHolograms() {
        if (location == null || location.getWorld() == null) return;
        despawnHolograms();

        interactStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        interactStand.setCustomName("§7");
        interactStand.setCustomNameVisible(false);
        interactStand.setGravity(false);
        interactStand.setVisible(false);

        Location holoLoc = location.clone().add(0, 1.15, 0);
        for (int i = hologramLines.size() - 1; i >= 0; i--) {
            String line = hologramLines.get(i);
            ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
            stand.setCustomName(line);
            stand.setCustomNameVisible(true);
            stand.setGravity(false);
            stand.setVisible(false);
            stand.setSmall(true);
            hologramStands.add(0, stand);

            holoLoc.add(0, 0.28, 0);
        }
    }

    public void updateHologramLine(int lineIndex, String text) {
        if (lineIndex >= 0 && lineIndex < hologramStands.size()) {
            hologramStands.get(lineIndex).setCustomName(text);
        }
    }

    public void despawnHolograms() {
        if (interactStand != null && !interactStand.isDead()) {
            interactStand.remove();
            interactStand = null;
        }
        for (ArmorStand stand : hologramStands) {
            if (stand != null && !stand.isDead()) {
                stand.remove();
            }
        }
        hologramStands.clear();
    }

    private void applySkin(Object profile, String name) {
        try {
            String[] skinData = SkinFetcher.getSkin(name);
            if (skinData != null) {
                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                Constructor<?> propConstructor = propertyClass.getConstructor(String.class, String.class, String.class);
                Object propertyObj = propConstructor.newInstance("textures", skinData[0], skinData[1]);

                Method getPropertiesMethod = profile.getClass().getMethod("getProperties");
                Object propertyMap = getPropertiesMethod.invoke(profile);
                Method putMethod = propertyMap.getClass().getMethod("put", Object.class, Object.class);
                putMethod.invoke(propertyMap, "textures", propertyObj);
            }
        } catch (Exception ignored) {}
    }
}
