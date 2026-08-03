package com.ascend.core.spigot;

import co.aikar.commands.PaperCommandManager;
import com.ascend.core.api.Constants;
import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.tag.Tag;
import com.ascend.core.spigot.command.GamemodeCommand;
import com.ascend.core.spigot.command.TagCommand;
import com.ascend.core.spigot.listener.ChatListener;
import com.ascend.core.spigot.listener.PlayerListener;
import com.ascend.core.spigot.redis.RedisPubSubListener;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.LoggerFactory;

@Getter
public class CoreSpigotPlugin extends JavaPlugin {

    @Getter
    private static CoreSpigotPlugin instance;

    private Connect mongoConnection;
    private RedisConnect redisConnection;
    private PaperCommandManager commandManager;
    private RedisPubSubListener redisPubSubListener;

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

        redisPubSubListener = new RedisPubSubListener(redisConnection);

        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        registerCommands();

        getLogger().info("enabled.");
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);

        commandManager.getCommandContexts().registerContext(Tag.class, c -> {
            Tag tag = Tag.fromName(c.popFirstArg());
            if (tag == null) throw new co.aikar.commands.InvalidCommandArgument("§cTag não encontrada.", false);
            return tag;
        });

        commandManager.registerCommand(new TagCommand());
        commandManager.registerCommand(new GamemodeCommand());
    }

    @Override
    public void onDisable() {
        if (mongoConnection != null) mongoConnection.close();
        if (redisConnection != null) redisConnection.close();
        getLogger().info("disabled.");
    }
}
