package com.ascend.core.spigot.redis;

import com.ascend.core.api.account.Account;
import com.ascend.core.api.account.AccountCache;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.rank.Rank;
import com.ascend.core.api.tag.Tag;
import com.ascend.core.spigot.CoreSpigotPlugin;
import com.ascend.core.spigot.listener.PlayerListener;
import com.ascend.core.spigot.tag.TagManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;

public class RedisPubSubListener {

    private final RedisConnect redisConnect;

    public RedisPubSubListener(RedisConnect redisConnect) {
        this.redisConnect = redisConnect;
        startListener();
    }

    private void startListener() {
        if (redisConnect == null || !redisConnect.isConnected()) return;

        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = redisConnect.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if ("ascend:rank_update".equalsIgnoreCase(channel)) {
                            handleRankUpdate(message);
                        }
                    }
                }, "ascend:rank_update");
            } catch (Exception ignored) {}
        });
    }

    private void handleRankUpdate(String message) {
        if (message == null || message.isBlank()) return;
        String[] parts = message.split(":");
        if (parts.length < 3) return;

        String username = parts[0];
        try {
            Rank newRank = Rank.valueOf(parts[1]);
            Tag newTag = Tag.fromName(parts[2]);

            Account account = AccountCache.getAccount(username);
            if (account != null) {
                account.setRank(newRank);
                account.setTag(newTag);
            }

            Bukkit.getScheduler().runTask(CoreSpigotPlugin.getInstance(), () -> {
                Player player = Bukkit.getPlayerExact(username);
                if (player != null && player.isOnline()) {
                    PlayerListener.updatePlayerPermissions(player, newRank);
                    if (newTag != null) {
                        TagManager.applyTag(player, newTag);
                    }
                }
            });
        } catch (Exception ignored) {}
    }
}
