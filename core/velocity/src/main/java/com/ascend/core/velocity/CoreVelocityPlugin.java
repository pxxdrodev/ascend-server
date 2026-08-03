package com.ascend.core.velocity;

import com.ascend.core.api.Constants;
import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.rank.Rank;
import com.ascend.core.velocity.command.GroupCommand;
import com.ascend.core.velocity.command.StaffChatCommand;
import com.ascend.core.velocity.listener.StaffChatListener;
import com.ascend.core.velocity.utils.ServerMotd;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.bson.Document;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;

import javax.inject.Inject;
import java.util.Locale;

@Plugin(
        id          = "core",
        name        = "Core Velocity",
        version     = "1.0.0",
        authors     = {"PxxdroDev", "LuisSantini"}
)
@Getter
public class CoreVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private Connect mongoConnection;
    private RedisConnect redisConnection;

    @Inject
    public CoreVelocityPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        mongoConnection = new Connect(logger, Constants.MONGO_URI, Constants.DATABASE_NAME);
        mongoConnection.connect();

        redisConnection = new RedisConnect(logger, Constants.REDIS_HOST, Constants.REDIS_PORT, Constants.REDIS_PASSWORD);
        redisConnection.connect();

        StaffChatCommand staffChatCommand = new StaffChatCommand(this);

        server.getEventManager().register(this, new ServerMotd());
        server.getEventManager().register(this, new StaffChatListener(this, staffChatCommand));

        registerCommands(staffChatCommand);

        logger.info("enabled.");
    }

    private void registerCommands(StaffChatCommand staffChatCommand) {
        CommandManager manager = server.getCommandManager();

        CommandMeta groupMeta = manager.metaBuilder("group")
                .aliases("setgroup", "setrank", "rankset")
                .plugin(this)
                .build();
        manager.register(groupMeta, new GroupCommand(this));

        CommandMeta scMeta = manager.metaBuilder("sc")
                .aliases("staffchat")
                .plugin(this)
                .build();
        manager.register(scMeta, staffChatCommand);
    }

    public Rank getPlayerRank(CommandSource source) {
        if (!(source instanceof Player)) {
            return Rank.ADMIN;
        }

        Player player = (Player) source;
        String key = "player:" + player.getUsername().toLowerCase(Locale.ROOT);

        if (redisConnection != null && redisConnection.isConnected()) {
            try (Jedis jedis = redisConnection.getResource()) {
                String rankStr = jedis.hget(key, "rank");
                if (rankStr != null) {
                    try {
                        return Rank.valueOf(rankStr.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (Exception ignored) {}
        }

        if (mongoConnection != null && mongoConnection.isConnected()) {
            try {
                MongoCollection<Document> col = mongoConnection.getDatabase().getCollection("accounts");
                Document doc = col.find(Filters.eq("username_lower", player.getUsername().toLowerCase(Locale.ROOT))).first();
                if (doc != null && doc.containsKey("rank")) {
                    try {
                        Rank rank = Rank.valueOf(doc.getString("rank").toUpperCase(Locale.ROOT));
                        if (redisConnection != null && redisConnection.isConnected()) {
                            try (Jedis jedis = redisConnection.getResource()) {
                                jedis.hset(key, "rank", rank.name());
                            } catch (Exception ignored) {}
                        }
                        return rank;
                    } catch (IllegalArgumentException ignored) {}
                }
            } catch (Exception ignored) {}
        }

        return Rank.DEFAULT;
    }

    public void shutdown() {
        if (mongoConnection != null) mongoConnection.close();
        if (redisConnection != null) redisConnection.close();
        logger.info("disabled.");
    }
}
