package com.ascend.core.velocity;

import com.ascend.core.api.Constants;
import com.ascend.core.api.account.Account;
import com.ascend.core.api.account.AccountCache;
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
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bson.Document;
import org.slf4j.Logger;
import redis.clients.jedis.Jedis;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
    private Connect mongoConnection;
    private RedisConnect redisConnection;

    private final List<String[]> autoMessages = List.of(
            new String[]{
                    "§b§lASCEND §8» §eAdquira §6ranks incríveis §ee muito mais em nossa loja!",
                    " §b ascendstudios.net/loja"
            },
            new String[]{
                    "§b§lASCEND §8» §ePrecisa de suporte ou quer trocar uma ideia? Entre em nosso Discord!",
                    " §b discord.ascendstudios.net"
            },
            new String[]{
                    "§b§lASCEND §8» §eFique por dentro das novidades e atualizações da nossa rede!",
                    " §b ascendstudios.net"
            }
    );
    private final AtomicInteger autoMessageIndex = new AtomicInteger(0);

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

        server.getScheduler().buildTask(this, () -> {
            if (redisConnection != null && redisConnection.isConnected()) {
                try (Jedis jedis = redisConnection.getResource()) {
                    jedis.set("global:online", String.valueOf(server.getPlayerCount()));

                    server.getAllServers().forEach(registeredServer -> {
                        String sName = registeredServer.getServerInfo().getName().toLowerCase(Locale.ROOT);
                        int count = registeredServer.getPlayersConnected().size();
                        jedis.hset("servers:online", sName, String.valueOf(count));
                    });
                } catch (Exception ignored) {}
            }
        }).repeat(2, TimeUnit.SECONDS).schedule();

        server.getScheduler().buildTask(this, () -> {
            if (server.getPlayerCount() == 0) return;

            int index = Math.abs(autoMessageIndex.getAndIncrement() % autoMessages.size());
            String[] lines = autoMessages.get(index);

            for (Player p : server.getAllPlayers()) {
                for (String line : lines) {
                    p.sendMessage(serializer.deserialize(line.replace('&', '§')));
                }
            }
        }).delay(10, TimeUnit.SECONDS).repeat(60, TimeUnit.SECONDS).schedule();

        logger.info("enabled.");
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        server.getScheduler().buildTask(this, () -> {
            AccountCache.loadAccount(
                    event.getPlayer().getUniqueId(),
                    event.getPlayer().getUsername(),
                    redisConnection,
                    mongoConnection
            );
        }).schedule();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        AccountCache.invalidate(event.getPlayer().getUniqueId(), event.getPlayer().getUsername());
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

        Account account = AccountCache.getAccount(player.getUniqueId());
        if (account != null && account.getRank() != null) {
            return account.getRank();
        }

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

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (mongoConnection != null) mongoConnection.close();
        if (redisConnection != null) redisConnection.close();
        logger.info("disabled.");
    }
}
