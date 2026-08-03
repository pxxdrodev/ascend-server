package com.ascend.lobby.spigot.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.ascend.core.api.game.ServerType;
import com.ascend.core.api.rank.Rank;
import com.ascend.lobby.api.utils.Utils;
import com.ascend.lobby.spigot.LobbyPlugin;
import com.ascend.lobby.spigot.npc.NPCManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@CommandAlias("setar")
public class SetarCommand extends BaseCommand {

    @Subcommand("spawn")
    public void onSetSpawn(Player player) {
        if (!hasAdmin(player)) return;

        LobbyPlugin plugin = LobbyPlugin.getInstance();
        plugin.getConfig().set("config.locations.spawn", Utils.serializeLocation(player.getLocation()));
        plugin.saveConfig();

        player.sendMessage(color("&aO spawn foi setado com sucesso."));
    }

    @Subcommand("npc")
    @Syntax("<infectionsoup>")
    public void onSetNPC(Player player, @Optional String type) {
        if (!hasAdmin(player)) return;

        if (type == null || !type.equalsIgnoreCase("infectionsoup")) {
            player.sendMessage(color("&cSintaxe incorreta, utilize '/setar npc infectionsoup'."));
            return;
        }

        Location loc = player.getLocation();
        LobbyPlugin plugin = LobbyPlugin.getInstance();
        plugin.getConfig().set("config.locations.npcs.infectionsoup.location", Utils.serializeLocation(loc));
        plugin.saveConfig();

        NPCManager npcManager = plugin.getNpcManager();
        if (npcManager != null) {
            String skin = plugin.getConfig().getString("config.locations.npcs.infectionsoup.skin");
            npcManager.updateOrCreateNPC("infectionsoup", ServerType.INFECTIONSOUP, loc, skin, "§2§lINFECTION SOUP");
            player.sendMessage(color("&aO NPC de InfectionSoup foi gerado com sucesso!"));
        }
    }

    @Subcommand("skin")
    @Syntax("<infectionsoup> <nick>")
    public void onSetSkin(Player player, @Optional String type, @Optional String skinName) {
        if (!hasAdmin(player)) return;

        if (type == null || skinName == null || !type.equalsIgnoreCase("infectionsoup")) {
            player.sendMessage(color("&cSintaxe incorreta, utilize '/setar skin infectionsoup <nick>'."));
            return;
        }

        LobbyPlugin plugin = LobbyPlugin.getInstance();
        plugin.getConfig().set("config.locations.npcs.infectionsoup.skin", skinName);
        plugin.saveConfig();

        NPCManager npcManager = plugin.getNpcManager();
        if (npcManager != null) {
            String locStr = plugin.getConfig().getString("config.locations.npcs.infectionsoup.location");
            Location loc = Utils.deserializeLocation(locStr);
            if (loc != null) {
                npcManager.updateOrCreateNPC("infectionsoup", ServerType.INFECTIONSOUP, loc, skinName, "§2§lINFECTION SOUP");
            }
        }

        player.sendMessage(color("&aSkin do NPC de InfectionSoup alterada para '" + skinName + "' com sucesso."));
    }

    private boolean hasAdmin(Player player) {
        Rank rank = LobbyPlugin.getInstance().getPlayerRank(player);
        if (rank != Rank.ADMIN) {
            player.sendMessage(color("&cVocê não tem permissão para executar este comando."));
            return false;
        }
        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
