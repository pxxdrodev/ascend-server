package com.ascend.game.spigot.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.ascend.core.api.utils.LocationUtils;
import com.ascend.game.spigot.InfectionSoupPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandAlias("setar")
public class SetarCommand extends BaseCommand {

    @Subcommand("spawn")
    public void onSetSpawn(Player player) {
        if (!player.hasPermission("infectionsoup.admin")) {
            player.sendMessage(color("&cVocê não tem permissão para executar este comando."));
            return;
        }

        InfectionSoupPlugin plugin = InfectionSoupPlugin.getInstance();
        plugin.getConfig().set("config.locations.spawn", LocationUtils.serializeLocation(player.getLocation()));
        plugin.saveConfig();

        player.sendMessage(color("&aO spawn de espera do InfectionSoup foi setado com sucesso."));
    }

    @Subcommand("arena")
    public void onSetArena(Player player) {
        if (!player.hasPermission("infectionsoup.admin")) {
            player.sendMessage(color("&cVocê não tem permissão para executar este comando."));
            return;
        }

        InfectionSoupPlugin plugin = InfectionSoupPlugin.getInstance();
        plugin.getConfig().set("config.locations.arena", LocationUtils.serializeLocation(player.getLocation()));
        plugin.saveConfig();

        player.sendMessage(color("&aO spawn da arena do InfectionSoup foi setado com sucesso."));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
