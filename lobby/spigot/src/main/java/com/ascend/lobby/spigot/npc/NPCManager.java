package com.ascend.lobby.spigot.npc;

import com.ascend.core.api.game.ServerType;
import com.ascend.lobby.spigot.LobbyPlugin;
import com.ascend.lobby.spigot.utils.Utils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class NPCManager {

    private final LobbyPlugin plugin;
    private final List<CoreNPC> npcs = new ArrayList<>();

    public NPCManager(LobbyPlugin plugin) {
        this.plugin = plugin;
        setupNPCs();
        startUpdateTask();
    }

    public void setupNPCs() {
        despawnAll();

        CoreNPC infection = createNPC(
                "infectionsoup",
                ServerType.INFECTIONSOUP,
                "infectionsoup",
                "config.locations.npcs.infectionsoup.location",
                "config.locations.npcs.infectionsoup.skin",
                "§2§lINFECTION SOUP",
                "§e0 jogando agora!"
        );

        if (infection != null) npcs.add(infection);

        spawnHologramsAll();
    }

    public void updateOrCreateNPC(String id, ServerType type, Location location, String skinName, String displayName) {
        despawnAll();
        CoreNPC npc = new CoreNPC(id, type, id, location, skinName);
        npc.setHologramLines(Arrays.asList(displayName, "§e0 jogando agora!"));
        npcs.add(npc);
        spawnHologramsAll();

        for (Player p : Bukkit.getOnlinePlayers()) {
            renderNPCsFor(p);
        }
    }

    private CoreNPC createNPC(String id, ServerType type, String serverName, String locPath, String skinPath, String... lines) {
        String rawLoc = plugin.getConfig().getString(locPath);
        Location loc = Utils.deserializeLocation(rawLoc);
        if (loc == null) return null;

        String skinName = plugin.getConfig().getString(skinPath);
        CoreNPC npc = new CoreNPC(id, type, serverName, loc, skinName);
        npc.setHologramLines(Arrays.asList(lines));
        return npc;
    }

    public void renderNPCsFor(Player player) {
        for (CoreNPC npc : npcs) {
            npc.spawnFor(player);
        }
    }

    public void spawnHologramsAll() {
        for (CoreNPC npc : npcs) {
            npc.spawnHolograms();
        }
    }

    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (CoreNPC npc : npcs) {
                int onlineCount = getOnlinePlayersForServer(npc.getServerName());
                npc.updateHologramLine(1, "§e" + onlineCount + " jogando agora!");
            }
        }, 100L, 100L);
    }

    private int getOnlinePlayersForServer(String serverName) {
        return 0;
    }

    public void despawnAll() {
        for (CoreNPC npc : npcs) {
            npc.despawnHolograms();
        }
        npcs.clear();
    }
}
