package com.ascend.lobby.api.listener;

import com.ascend.lobby.api.bossbar.BossBarManager;
import com.ascend.lobby.api.title.TitleManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class LobbyPlayerListener implements Listener {

    private final Plugin plugin;
    private final BossBarManager bossBarManager;

    public LobbyPlayerListener(Plugin plugin) {
        this.plugin = plugin;
        this.bossBarManager = new BossBarManager(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();
        TitleManager.sendRandomWelcomeTitle(player);
        bossBarManager.addPlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        bossBarManager.removePlayer(event.getPlayer());
    }
}
