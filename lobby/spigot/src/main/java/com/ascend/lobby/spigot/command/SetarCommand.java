package com.ascend.lobby.spigot.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.ascend.core.api.game.ServerType;
import com.ascend.core.api.rank.Rank;
import com.ascend.lobby.spigot.LobbyPlugin;
import com.ascend.lobby.spigot.npc.NPCManager;
import com.ascend.lobby.spigot.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

@CommandAlias("setar")
public class SetarCommand extends BaseCommand {

    @Subcommand("spawn")
    public void onSetSpawn(Player player) {
        if (!hasAdmin(player)) return;

        File configFile = new File(LobbyPlugin.getInstance().getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("config.locations.spawn", Utils.serializeLocation(player.getLocation()));
        saveConfig(configFile, config);

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
        File configFile = new File(LobbyPlugin.getInstance().getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("config.locations.npcs.infectionsoup.location", Utils.serializeLocation(loc));
        saveConfig(configFile, config);

        NPCManager npcManager = LobbyPlugin.getInstance().getNpcManager();
        if (npcManager != null) {
            String skin = config.getString("config.locations.npcs.infectionsoup.skin");
            npcManager.updateOrCreateNPC("infectionsoup", ServerType.INFECTIONSOUP, loc, skin, "§b§lINFECTION SOUP");
            player.sendMessage(color("&aO NPC de InfectionSoup foi gerado com sucesso!"));
        } else {
            player.sendMessage(color("&eLocal salvo no config.yml! (Aviso: Nenhum plugin de NPC como zNPCs ou Citizens foi detectado ativo no servidor)."));
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

        File configFile = new File(LobbyPlugin.getInstance().getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("config.locations.npcs.infectionsoup.skin", skinName);
        saveConfig(configFile, config);

        NPCManager npcManager = LobbyPlugin.getInstance().getNpcManager();
        if (npcManager != null) {
            String locStr = config.getString("config.locations.npcs.infectionsoup.location");
            Location loc = Utils.deserializeLocation(locStr);
            if (loc != null) {
                npcManager.updateOrCreateNPC("infectionsoup", ServerType.INFECTIONSOUP, loc, skinName, "§b§lINFECTION SOUP");
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

    private void saveConfig(File file, YamlConfiguration config) {
        try {
            config.save(file);
        } catch (IOException e) {
            LobbyPlugin.getInstance().getLogger().severe("Erro ao salvar config.yml: " + e.getMessage());
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
