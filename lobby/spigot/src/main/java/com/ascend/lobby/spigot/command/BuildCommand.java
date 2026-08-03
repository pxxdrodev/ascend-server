package com.ascend.lobby.spigot.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.ascend.core.api.rank.Rank;
import com.ascend.lobby.api.build.BuildManager;
import com.ascend.lobby.spigot.LobbyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

@CommandAlias("build")
public class BuildCommand extends BaseCommand {

    @Default
    @Syntax("[on/off]")
    public void onBuild(Player player, @Optional String mode) {
        Rank rank = LobbyPlugin.getInstance().getPlayerRank(player);
        if (rank != Rank.ADMIN) {
            player.sendMessage(color("&cVocê não tem permissão para executar este comando."));
            return;
        }

        if (mode == null || mode.isEmpty()) {
            player.sendMessage(color("&cSintaxe incorreta, utilize '/build [on/off]'."));
            return;
        }

        if (mode.equalsIgnoreCase("on")) {
            if (BuildManager.isBuilder(player.getName())) {
                player.sendMessage(color("&cVocê já está com o modo construtor ativado."));
                return;
            }
            BuildManager.addBuilder(player.getName());
            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage(color("&aModo construtor foi ativado."));
        } else if (mode.equalsIgnoreCase("off")) {
            if (!BuildManager.isBuilder(player.getName())) {
                player.sendMessage(color("&cVocê já está com o modo construtor desativado."));
                return;
            }
            BuildManager.removeBuilder(player.getName());
            player.setGameMode(GameMode.ADVENTURE);
            player.sendMessage(color("&aModo construtor foi desativado."));
        } else {
            player.sendMessage(color("&cSintaxe incorreta, utilize '/build [on/off]'."));
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
