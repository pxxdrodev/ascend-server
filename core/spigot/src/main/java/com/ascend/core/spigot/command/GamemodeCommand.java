package com.ascend.core.spigot.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.ascend.core.api.rank.Rank;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

@CommandAlias("gm|gamemode")
public class GamemodeCommand extends BaseCommand {

    @Default
    @Syntax("[0/1/2/3] [jogador]")
    public void onGamemode(Player player, @Optional String modeArg, @Optional Player target) {
        if (!hasPermission(player)) {
            player.sendMessage(color("&cVocê não tem permissão para executar este comando."));
            return;
        }

        if (modeArg == null || modeArg.isEmpty()) {
            player.sendMessage(color("&cSintaxe incorreta, utilize '/gm [0/1/2/3]'."));
            return;
        }

        GameMode gm = parseGamemode(modeArg);
        if (gm == null) {
            player.sendMessage(color("&cSintaxe incorreta, utilize '/gm [0/1/2/3]'."));
            return;
        }

        Player recipient = target != null ? target : player;
        recipient.setGameMode(gm);
        String name = getModeName(gm);

        if (recipient.equals(player)) {
            player.sendMessage(color("&aSeu modo de jogo foi alterado para &f" + name + "&a."));
        } else {
            player.sendMessage(color("&aModo de jogo de &f" + recipient.getName() + " &aalterado para &f" + name + "&a."));
            recipient.sendMessage(color("&aSeu modo de jogo foi alterado para &f" + name + "&a."));
        }
    }

    private boolean hasPermission(Player player) {
        if (player.isOp()) return true;
        for (Rank r : Rank.values()) {
            if (r.ordinal() <= Rank.MOD.ordinal() &&
                !r.getPermission().isEmpty() &&
                player.hasPermission(r.getPermission())) {
                return true;
            }
        }
        return false;
    }

    private GameMode parseGamemode(String arg) {
        switch (arg.toLowerCase()) {
            case "0": case "s": case "survival":   return GameMode.SURVIVAL;
            case "1": case "c": case "creative":   return GameMode.CREATIVE;
            case "2": case "a": case "adventure":  return GameMode.ADVENTURE;
            case "3": case "sp": case "spectator": return GameMode.SPECTATOR;
            default: return null;
        }
    }

    private String getModeName(GameMode mode) {
        switch (mode) {
            case SURVIVAL:   return "Sobrevivência";
            case CREATIVE:   return "Criativo";
            case ADVENTURE:  return "Aventura";
            case SPECTATOR:  return "Espectador";
            default:         return mode.name();
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
