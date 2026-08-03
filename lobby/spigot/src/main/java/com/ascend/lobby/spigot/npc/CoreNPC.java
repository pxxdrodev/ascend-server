package com.ascend.lobby.spigot.npc;

import com.ascend.core.api.game.ServerType;
import com.ascend.core.api.nms.NMSHelper;
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
    private boolean initialized = false;

    public CoreNPC(String id, ServerType serverType, String serverName, Location location, String skinName) {
        this.id = id;
        this.serverType = serverType;
        this.serverName = serverName;
        this.location = location;
        this.skinName = skinName != null && !skinName.trim().isEmpty() ? skinName : "Steve";
        this.npcUuid = UUID.randomUUID();

        SkinFetcher.fetchSkinAsync(this.skinName, () -> {
            Bukkit.getScheduler().runTask(LobbyPlugin.getInstance(), () -> {
                initNMSEntity();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    spawnFor(p);
                }
            });
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void initNMSEntity() {
        if (location == null || location.getWorld() == null) return;

        try {
            Class<?> craftServerClass = NMSHelper.getCraftClass("CraftServer");
            Class<?> craftWorldClass = NMSHelper.getCraftClass("CraftWorld");
            Class<?> entityPlayerClass = NMSHelper.getNMSClass("EntityPlayer");
            Class<?> minecraftServerClass = NMSHelper.getNMSClass("MinecraftServer");
            Class<?> worldClass = NMSHelper.getNMSClass("World");
            Class<?> worldServerClass = NMSHelper.getNMSClass("WorldServer");
            Class<?> playerInteractClass = NMSHelper.getNMSClass("PlayerInteractManager");
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");

            Object nmsServer = craftServerClass.getMethod("getServer").invoke(Bukkit.getServer());
            Object nmsWorld = craftWorldClass.getMethod("getHandle").invoke(location.getWorld());

            Constructor<?> profileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
            Object profile = profileConstructor.newInstance(npcUuid, " ");

            String[] skinData = SkinFetcher.getSkin(skinName);
            if (skinData != null && skinData.length >= 2) {
                Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
                Constructor<?> propConstructor = propertyClass.getConstructor(String.class, String.class, String.class);
                Object propertyObj = propConstructor.newInstance("textures", skinData[0], skinData[1]);

                Method getPropertiesMethod = profile.getClass().getMethod("getProperties");
                Object propertyMap = getPropertiesMethod.invoke(profile);
                Method putMethod = propertyMap.getClass().getMethod("put", Object.class, Object.class);
                putMethod.invoke(propertyMap, "textures", propertyObj);
            }

            Constructor<?> interactConstructor = playerInteractClass.getConstructor(worldClass);
            Object interactManager = interactConstructor.newInstance(nmsWorld);

            Constructor<?> entityConstructor = entityPlayerClass.getConstructor(minecraftServerClass, worldServerClass, gameProfileClass, playerInteractClass);
            nmsEntityPlayer = entityConstructor.newInstance(nmsServer, nmsWorld, profile, interactManager);

            Method setLocMethod = entityPlayerClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            setLocMethod.invoke(nmsEntityPlayer, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            this.entityId = (int) entityPlayerClass.getMethod("getId").invoke(nmsEntityPlayer);
            this.initialized = true;

        } catch (Throwable t) {
            Bukkit.getLogger().warning("Erro ao inicializar NMS EntityPlayer para " + id + ": " + t.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void spawnFor(Player player) {
        if (location == null || location.getWorld() == null || player == null) return;

        if (!initialized || nmsEntityPlayer == null) {
            SkinFetcher.fetchSkinAsync(skinName, () -> {
                Bukkit.getScheduler().runTask(LobbyPlugin.getInstance(), () -> {
                    if (!initialized) initNMSEntity();
                    spawnFor(player);
                });
            });
            return;
        }

        try {
            Class<?> entityPlayerClass = NMSHelper.getNMSClass("EntityPlayer");
            Class<?> packetInfoClass = NMSHelper.getNMSClass("PacketPlayOutPlayerInfo");
            Class<?> enumInfoActionClass = NMSHelper.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
            Class<?> packetSpawnClass = NMSHelper.getNMSClass("PacketPlayOutNamedEntitySpawn");
            Class<?> dataWatcherClass = NMSHelper.getNMSClass("DataWatcher");
            Class<?> packetMetadataClass = NMSHelper.getNMSClass("PacketPlayOutEntityMetadata");
            Class<?> packetHeadRotationClass = NMSHelper.getNMSClass("PacketPlayOutEntityHeadRotation");

            Object addAction = Enum.valueOf((Class<Enum>) enumInfoActionClass, "ADD_PLAYER");
            Constructor<?> infoConstructor = packetInfoClass.getConstructor(enumInfoActionClass, Iterable.class);
            Object infoPacket = infoConstructor.newInstance(addAction, Collections.singletonList(nmsEntityPlayer));

            Constructor<?> spawnConstructor = packetSpawnClass.getConstructor(NMSHelper.getNMSClass("EntityHuman"));
            Object spawnPacket = spawnConstructor.newInstance(nmsEntityPlayer);

            Object watcher = entityPlayerClass.getMethod("getDataWatcher").invoke(nmsEntityPlayer);
            dataWatcherClass.getMethod("watch", int.class, Object.class).invoke(watcher, 10, (byte) 127);

            Constructor<?> metaConstructor = packetMetadataClass.getConstructor(int.class, dataWatcherClass, boolean.class);
            Object metaPacket = metaConstructor.newInstance(this.entityId, watcher, true);

            byte yawByte = (byte) (location.getYaw() * 256.0F / 360.0F);
            Constructor<?> headRotConstructor = packetHeadRotationClass.getConstructor(NMSHelper.getNMSClass("Entity"), byte.class);
            Object headRotPacket = headRotConstructor.newInstance(nmsEntityPlayer, yawByte);

            NMSHelper.sendPacket(player, infoPacket);
            NMSHelper.sendPacket(player, spawnPacket);
            NMSHelper.sendPacket(player, metaPacket);
            NMSHelper.sendPacket(player, headRotPacket);

            Bukkit.getScheduler().runTaskLater(LobbyPlugin.getInstance(), () -> {
                try {
                    Object removeAction = Enum.valueOf((Class<Enum>) enumInfoActionClass, "REMOVE_PLAYER");
                    Object removePacket = infoConstructor.newInstance(removeAction, Collections.singletonList(nmsEntityPlayer));
                    NMSHelper.sendPacket(player, removePacket);
                } catch (Exception ignored) {}
            }, 40L);

        } catch (Throwable t) {
            Bukkit.getLogger().warning("Erro ao enviar pacotes do NPC para " + player.getName() + ": " + t.getMessage());
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
}
