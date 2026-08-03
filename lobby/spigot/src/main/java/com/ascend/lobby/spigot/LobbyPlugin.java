package com.ascend.lobby.spigot;

import co.aikar.commands.PaperCommandManager;
import com.ascend.core.api.Constants;
import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.rank.Rank;
import com.ascend.lobby.api.listener.LobbyPlayerListener;
import com.ascend.lobby.api.listener.LobbyProtectionListener;
import com.ascend.lobby.spigot.command.BuildCommand;
import com.ascend.lobby.spigot.command.SetarCommand;
import com.ascend.lobby.spigot.listener.LobbyListener;
import com.ascend.lobby.spigot.npc.NPCManager;
import com.ascend.lobby.spigot.npc.listener.NPCListener;
import com.ascend.lobby.spigot.scoreboard.LobbyScoreboardManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Locale;

@Getter
public class LobbyPlugin extends JavaPlugin {

    @Getter
    private static LobbyPlugin instance;

    private Connect mongoConnection;
    private RedisConnect redisConnection;
    private LobbyScoreboardManager scoreboardManager;
    private NPCManager npcManager;
    private PaperCommandManager commandManager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        mongoConnection = new Connect(LoggerFactory.getLogger("MongoDB"), Constants.MONGO_URI, Constants.DATABASE_NAME);
        mongoConnection.connect();

        redisConnection = new RedisConnect(LoggerFactory.getLogger("Redis"), Constants.REDIS_HOST, Constants.REDIS_PORT, Constants.REDIS_PASSWORD);
        redisConnection.connect();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        World world = Bukkit.getWorld("world");
        if (world != null) {
            world.setAutoSave(false);
            world.setGameRuleValue("doDaylightCycle", "false");
            world.setGameRuleValue("keepInventory", "true");
            world.setGameRuleValue("randomTickSpeed", "0");
        }

        saveDefaultConfig();

        commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new BuildCommand());
        commandManager.registerCommand(new SetarCommand());

        scoreboardManager = new LobbyScoreboardManager(this);
        scoreboardManager.startTasks();

        npcManager = new NPCManager(this);

        getServer().getPluginManager().registerEvents(new LobbyPlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(scoreboardManager), this);
        getServer().getPluginManager().registerEvents(new NPCListener(this), this);

        getLogger().info("enabled.");
    }

    public Rank getPlayerRank(Player player) {
        String userLower = player.getName().toLowerCase(Locale.ROOT);
        if (redisConnection != null && redisConnection.isConnected()) {
            try (Jedis jedis = redisConnection.getResource()) {
                String r = jedis.hget("player:" + userLower, "rank");
                if (r != null) {
                    try { return Rank.valueOf(r.toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        if (mongoConnection != null && mongoConnection.isConnected()) {
            try {
                var doc = mongoConnection.getDatabase().getCollection("accounts")
                        .find(com.mongodb.client.model.Filters.eq("username_lower", userLower)).first();
                if (doc != null && doc.containsKey("rank")) {
                    try { return Rank.valueOf(doc.getString("rank").toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }

        return Rank.DEFAULT;
    }

    @Override
    public void onDisable() {
        if (npcManager != null) {
            npcManager.despawnAll();
        }
        if (mongoConnection != null) mongoConnection.close();
        if (redisConnection != null) redisConnection.close();
        getLogger().info("disabled.");
    }
}
