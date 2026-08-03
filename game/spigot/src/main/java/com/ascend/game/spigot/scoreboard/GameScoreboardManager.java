package com.ascend.game.spigot.scoreboard;

import com.ascend.game.api.InfectionSoupAPI;
import com.ascend.game.api.player.GamePlayer;
import com.ascend.game.api.state.GameState;
import com.ascend.game.api.team.Team;
import com.ascend.game.spigot.InfectionSoupPlugin;
import com.ascend.game.spigot.game.GameManager;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameScoreboardManager {

    private final InfectionSoupPlugin plugin;
    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    public GameScoreboardManager(InfectionSoupPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::updateAll, 20L, 20L);
    }

    public void addPlayer(Player player) {
        FastBoard board = new FastBoard(player);
        board.updateTitle("§2§lINFECTION SOUP");
        boards.put(player.getUniqueId(), board);
        updateBoard(player, board);
    }

    public void removePlayer(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = boards.get(player.getUniqueId());
            if (board == null) {
                board = new FastBoard(player);
                board.updateTitle("§2§lINFECTION SOUP");
                boards.put(player.getUniqueId(), board);
            }
            updateBoard(player, board);
        }
    }

    private void updateBoard(Player player, FastBoard board) {
        try {
            InfectionSoupAPI api = InfectionSoupAPI.getInstance();
            GameState state = api.getCurrentState();
            GameManager game = plugin.getGameManager();

            String dateStr = dateFormat.format(new Date());
            String timeStr = game != null ? game.getFormattedTime() : "00:00";

            int humans = 0;
            int infecteds = 0;
            for (GamePlayer gp : api.getPlayers().values()) {
                if (gp.getTeam() == Team.HUMAN) humans++;
                else if (gp.getTeam() == Team.INFECTED) infecteds++;
            }

            GamePlayer gp = api.getPlayer(player.getUniqueId());
            int kills = gp != null ? gp.getKills() : 0;
            String eventName = game != null && game.getCurrentEvent() != null ? game.getCurrentEvent().getDisplayName() : "Nenhum";

            if (state == GameState.WAITING || state == GameState.STARTING) {
                board.updateLines(
                        "§7" + dateStr,
                        "",
                        "§fEstado: §e" + state.getDisplayName(),
                        "§fJogadores: §a" + Bukkit.getOnlinePlayers().size() + "/16",
                        "",
                        "§fKit Humano: §a" + (gp != null ? gp.getHumanKit().getName() : "Soldado"),
                        "§fKit Infectado: §c" + (gp != null ? gp.getInfectedKit().getName() : "Runner"),
                        "",
                        "§eascendstudios.net"
                );
            } else {
                board.updateLines(
                        "§7" + dateStr,
                        "",
                        "§fTempo: §a" + timeStr,
                        "§fEstado: §e" + state.getDisplayName(),
                        "",
                        "§fHumanos: §a" + humans,
                        "§fInfectados: §c" + infecteds,
                        "",
                        "§fEvento: §e" + eventName,
                        "§fKills: §a" + kills,
                        "",
                        "§eascendstudios.net"
                );
            }
        } catch (Exception ignored) {}
    }
}
