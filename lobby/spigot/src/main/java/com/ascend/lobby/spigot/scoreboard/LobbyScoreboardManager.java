package com.ascend.lobby.spigot.scoreboard;

import com.ascend.core.api.level.LevelManager;
import com.ascend.core.api.rank.Rank;
import com.ascend.lobby.spigot.LobbyPlugin;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyScoreboardManager {

    private final LobbyPlugin plugin;
    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();
    private final ScoreboardAnimation animation = new ScoreboardAnimation();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    private volatile int cachedGlobalOnline = 1;

    public LobbyScoreboardManager(LobbyPlugin plugin) {
        this.plugin = plugin;
    }

    public void onJoin(Player player) {
        FastBoard board = new FastBoard(player);
        boards.put(player.getUniqueId(), board);
        updateBoard(player, board, cachedGlobalOnline);
    }

    public void onQuit(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    public void startTasks() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String title = animation.nextFrame();
            for (FastBoard board : boards.values()) {
                try {
                    board.updateTitle(title);
                } catch (Exception ignored) {}
            }
        }, 2L, 2L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            cachedGlobalOnline = fetchGlobalOnlineCount();

            for (Map.Entry<UUID, FastBoard> entry : boards.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    try {
                        updateBoard(player, entry.getValue(), cachedGlobalOnline);
                    } catch (Exception ignored) {}
                }
            }
        }, 20L, 20L);
    }

    private void updateBoard(Player player, FastBoard board, int online) {
        String currentDate = dateFormat.format(new Date());

        int level = LevelManager.getLevel(
                player.getName(),
                plugin.getRedisConnection(),
                plugin.getMongoConnection()
        );

        Rank rank = plugin.getPlayerRank(player);

        board.updateLines(
                "§7" + currentDate,
                "",
                "§fNível: §7[" + level + "§7]",
                "§fRank: " + rank.getColoredName(),
                "",
                "§fLobby: §7#01",
                "§fJogadores: §a" + online,
                "",
                "§eascendstudios.net"
        );
    }

    private int fetchGlobalOnlineCount() {
        if (plugin.getRedisConnection() != null && plugin.getRedisConnection().isConnected()) {
            try (Jedis jedis = plugin.getRedisConnection().getResource()) {
                String onlineStr = jedis.get("global:online");
                if (onlineStr != null) {
                    return Integer.parseInt(onlineStr);
                }
            } catch (Exception ignored) {}
        }
        return Bukkit.getOnlinePlayers().size();
    }
}
