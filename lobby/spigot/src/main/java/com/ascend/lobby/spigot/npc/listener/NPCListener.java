package com.ascend.lobby.spigot.npc.listener;

import com.ascend.lobby.spigot.LobbyPlugin;
import com.ascend.lobby.spigot.npc.CoreNPC;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class NPCListener implements Listener {

    private final LobbyPlugin plugin;
    private final Map<UUID, Long> connectCooldown = new HashMap<>();

    public NPCListener(LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getNpcManager() != null) {
            plugin.getNpcManager().renderNPCsFor(player);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (event.getRightClicked() != null) {
            checkAndConnectByLocation(player, event.getRightClicked().getLocation());
        }
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (event.getRightClicked() != null) {
            checkAndConnectByLocation(player, event.getRightClicked().getLocation());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            checkAndConnectByProximity(player);
        }
    }

    private void checkAndConnectByLocation(Player player, Location clickedLoc) {
        if (clickedLoc == null || clickedLoc.getWorld() == null || plugin.getNpcManager() == null) return;
        for (CoreNPC npc : plugin.getNpcManager().getNpcs()) {
            if (npc.getLocation() != null && npc.getLocation().getWorld().equals(clickedLoc.getWorld())) {
                if (npc.getLocation().distanceSquared(clickedLoc) <= 12.25D) {
                    tryConnect(player, npc);
                    return;
                }
            }
        }
    }

    private void checkAndConnectByProximity(Player player) {
        if (player == null || player.getWorld() == null || plugin.getNpcManager() == null) return;
        for (CoreNPC npc : plugin.getNpcManager().getNpcs()) {
            if (npc.getLocation() != null && npc.getLocation().getWorld().equals(player.getWorld())) {
                if (player.getLocation().distanceSquared(npc.getLocation()) <= 7.84D) {
                    tryConnect(player, npc);
                    return;
                }
            }
        }
    }

    private void tryConnect(Player player, CoreNPC npc) {
        if (connectCooldown.containsKey(player.getUniqueId()) && connectCooldown.get(player.getUniqueId()) > System.currentTimeMillis()) {
            return;
        }
        connectCooldown.put(player.getUniqueId(), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(2));
        connectServer(player, npc.getServerName());
    }

    private void connectServer(Player player, String serverName) {
        player.sendMessage(color("&aConectando ao servidor &f" + serverName + "&a..."));
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            byte[] data = out.toByteArray();

            try { player.sendPluginMessage(plugin, "BungeeCord", data); } catch (Exception ignored) {}
            try { player.sendPluginMessage(plugin, "bungeecord:main", data); } catch (Exception ignored) {}
            try { player.sendPluginMessage(plugin, "bungee:main", data); } catch (Exception ignored) {}
        } catch (Exception e) {
            player.sendMessage(color("&cNão foi possível conectar ao servidor &f" + serverName + "&c."));
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
