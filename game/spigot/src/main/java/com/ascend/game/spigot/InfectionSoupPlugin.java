package com.ascend.game.spigot;

import co.aikar.commands.PaperCommandManager;
import com.ascend.core.api.Constants;
import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.game.spigot.command.BuildCommand;
import com.ascend.game.spigot.command.GameCommand;
import com.ascend.game.spigot.command.SetarCommand;
import com.ascend.game.spigot.game.GameManager;
import com.ascend.game.spigot.listener.GameListener;
import com.ascend.game.spigot.listener.SoupListener;
import com.ascend.game.spigot.menu.KitMenu;
import com.ascend.game.spigot.scoreboard.GameScoreboardManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.LoggerFactory;

@Getter
public class InfectionSoupPlugin extends JavaPlugin {

    @Getter
    private static InfectionSoupPlugin instance;

    private Connect mongoConnection;
    private RedisConnect redisConnection;

    private GameManager gameManager;
    private GameScoreboardManager scoreboardManager;
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

        saveDefaultConfig();

        gameManager = new GameManager(this);
        scoreboardManager = new GameScoreboardManager(this);
        scoreboardManager.startTask();

        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new SoupListener(), this);
        getServer().getPluginManager().registerEvents(new KitMenu(), this);

        commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new SetarCommand());
        commandManager.registerCommand(new GameCommand(this));
        commandManager.registerCommand(new BuildCommand());

        getLogger().info("InfectionSoup minigame enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.stopGame();
        }
        if (mongoConnection != null) mongoConnection.close();
        if (redisConnection != null) redisConnection.close();
        getLogger().info("InfectionSoup minigame disabled.");
    }
}
