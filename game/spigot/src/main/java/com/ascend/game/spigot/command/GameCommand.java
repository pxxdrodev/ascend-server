package com.ascend.game.spigot.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.ascend.game.spigot.InfectionSoupPlugin;
import com.ascend.game.spigot.menu.KitMenu;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@CommandAlias("game|kit")
public class GameCommand extends BaseCommand {

    private final InfectionSoupPlugin plugin;

    public GameCommand(InfectionSoupPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    public void onKit(Player player) {
        KitMenu.openMenu(player);
    }

    @Subcommand("start|iniciar")
    public void onStart(Player player) {
        if (!player.hasPermission("infectionsoup.admin")) {
            player.sendMessage(color("&cVocê não tem permissão para executar este comando."));
            return;
        }

        if (plugin.getGameManager() != null) {
            plugin.getGameManager().startMainTicker();
            player.sendMessage(color("&aContagem regressiva da partida iniciada!"));
        }
    }

    @Subcommand("stop|parar")
    public void onStop(Player player) {
        if (!player.hasPermission("infectionsoup.admin")) {
            player.sendMessage(color("&cVocê não tem permissão para executar este comando."));
            return;
        }

        if (plugin.getGameManager() != null) {
            plugin.getGameManager().stopGame();
            player.sendMessage(color("&cA partida foi forçada a parar."));
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
