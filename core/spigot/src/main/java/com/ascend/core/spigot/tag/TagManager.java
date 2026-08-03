package com.ascend.core.spigot.tag;

import com.ascend.core.api.account.Account;
import com.ascend.core.api.account.AccountCache;
import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.rank.Rank;
import com.ascend.core.api.tag.Tag;
import com.ascend.core.spigot.CoreSpigotPlugin;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class TagManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagManager.class);

    private TagManager() {}

    public static void applyTag(Player player, Tag tag) {
        try {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

            for (Team t : board.getTeams()) {
                if (t.hasEntry(player.getName())) {
                    t.removeEntry(player.getName());
                }
            }

            String teamName = String.format("%02d", tag.ordinal()) + tag.name();
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }

            String prefix = tag.getPrefixColored();
            if (prefix.length() > 16) {
                prefix = prefix.substring(0, 16);
            }

            team.setPrefix(prefix);
            team.addEntry(player.getName());

            if (!player.getScoreboard().equals(board)) {
                player.setScoreboard(board);
            }

            player.setDisplayName(prefix + player.getName() + ChatColor.RESET);
            player.setPlayerListName(null);
        } catch (Exception e) {
            LOGGER.warn("Erro ao aplicar tag no scoreboard para {}: {}", player.getName(), e.getMessage());
            player.setDisplayName(tag.getPrefixColored() + player.getName() + ChatColor.RESET);
        }
    }

    public static Rank getPlayerRank(Player player) {
        Account account = AccountCache.getAccount(player.getUniqueId());
        if (account != null && account.getRank() != null) {
            return account.getRank();
        }

        String uuidStr = player.getUniqueId().toString();
        String rankName = redisGet("player:uuid:" + uuidStr, "rank");
        if (rankName == null) {
            rankName = redisGet("player:" + player.getName().toLowerCase(Locale.ROOT), "rank");
        }

        if (rankName != null) {
            return parseRank(rankName);
        }

        Document doc = findDocument(player);
        if (doc != null && doc.containsKey("rank")) {
            rankName = doc.getString("rank");
            redisSave("player:uuid:" + uuidStr, "rank", rankName);
            return parseRank(rankName);
        }
        return Rank.DEFAULT;
    }

    public static Tag getPlayerTag(Player player) {
        Account account = AccountCache.getAccount(player.getUniqueId());
        if (account != null && account.getTag() != null) {
            return account.getTag();
        }

        String uuidStr = player.getUniqueId().toString();
        String tagName = redisGet("player:uuid:" + uuidStr, "tag");
        if (tagName == null) {
            tagName = redisGet("player:" + player.getName().toLowerCase(Locale.ROOT), "tag");
        }

        if (tagName != null) {
            Tag tag = Tag.fromName(tagName);
            return tag != null ? tag : Tag.MEMBRO;
        }

        Document doc = findDocument(player);
        if (doc != null && doc.containsKey("tag")) {
            tagName = doc.getString("tag");
            redisSave("player:uuid:" + uuidStr, "tag", tagName);
            Tag tag = Tag.fromName(tagName);
            return tag != null ? tag : Tag.MEMBRO;
        }
        return Tag.MEMBRO;
    }

    public static void saveTag(Player player, Tag tag) {
        Account account = AccountCache.getAccount(player.getUniqueId());
        if (account != null) account.setTag(tag);

        String uuidStr = player.getUniqueId().toString();
        String tagName = tag.name();

        redisSave("player:uuid:" + uuidStr, "tag", tagName);
        redisSave("player:" + player.getName().toLowerCase(Locale.ROOT), "tag", tagName);

        CompletableFuture.runAsync(() -> {
            try {
                Connect mongo = CoreSpigotPlugin.getInstance().getMongoConnection();
                if (mongo == null || !mongo.isConnected()) return;
                MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                col.updateOne(
                        Filters.eq("uuid", uuidStr),
                        Updates.combine(
                                Updates.set("uuid", uuidStr),
                                Updates.set("username", player.getName()),
                                Updates.set("username_lower", player.getName().toLowerCase(Locale.ROOT)),
                                Updates.set("tag", tagName)
                        ),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                LOGGER.error("Erro ao salvar tag no MongoDB para {} (UUID {}): {}", player.getName(), uuidStr, e.getMessage());
            }
        });
    }

    public static void saveRank(Player player, Rank rank) {
        Account account = AccountCache.getAccount(player.getUniqueId());
        if (account != null) account.setRank(rank);

        String uuidStr = player.getUniqueId().toString();
        String rankName = rank.name();

        redisSave("player:uuid:" + uuidStr, "rank", rankName);
        redisSave("player:" + player.getName().toLowerCase(Locale.ROOT), "rank", rankName);

        CompletableFuture.runAsync(() -> {
            try {
                Connect mongo = CoreSpigotPlugin.getInstance().getMongoConnection();
                if (mongo == null || !mongo.isConnected()) return;
                mongo.getDatabase().getCollection("accounts").updateOne(
                        Filters.eq("uuid", uuidStr),
                        Updates.combine(
                                Updates.set("uuid", uuidStr),
                                Updates.set("username", player.getName()),
                                Updates.set("username_lower", player.getName().toLowerCase(Locale.ROOT)),
                                Updates.set("rank", rankName)
                        ),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                LOGGER.error("Erro ao salvar rank no MongoDB para {} (UUID {}): {}", player.getName(), uuidStr, e.getMessage());
            }
        });
    }

    public static void evict(Player player) {
        AccountCache.invalidate(player.getUniqueId(), player.getName());
        RedisConnect redis = CoreSpigotPlugin.getInstance().getRedisConnection();
        if (redis == null || !redis.isConnected()) return;
        try (Jedis jedis = redis.getResource()) {
            jedis.del("player:uuid:" + player.getUniqueId().toString());
            jedis.del("player:" + player.getName().toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            LOGGER.warn("Erro ao desalocar cache do Redis para {}: {}", player.getName(), e.getMessage());
        }
    }

    private static String redisGet(String key, String field) {
        RedisConnect redis = CoreSpigotPlugin.getInstance().getRedisConnection();
        if (redis == null || !redis.isConnected()) return null;
        try (Jedis jedis = redis.getResource()) {
            return jedis.hget(key, field);
        } catch (Exception e) {
            LOGGER.warn("Erro ao ler campo '{}' na chave Redis '{}': {}", field, key, e.getMessage());
            return null;
        }
    }

    private static void redisSave(String key, String field, String value) {
        RedisConnect redis = CoreSpigotPlugin.getInstance().getRedisConnection();
        if (redis == null || !redis.isConnected()) return;
        try (Jedis jedis = redis.getResource()) {
            jedis.hset(key, field, value);
        } catch (Exception e) {
            LOGGER.warn("Erro ao gravar campo '{}' na chave Redis '{}': {}", field, key, e.getMessage());
        }
    }

    private static Document findDocument(Player player) {
        Connect mongo = CoreSpigotPlugin.getInstance().getMongoConnection();
        if (mongo == null || !mongo.isConnected()) return null;
        try {
            Document doc = mongo.getDatabase()
                        .getCollection("accounts")
                        .find(Filters.eq("uuid", player.getUniqueId().toString()))
                        .first();
            if (doc == null) {
                doc = mongo.getDatabase()
                        .getCollection("accounts")
                        .find(Filters.eq("username_lower", player.getName().toLowerCase(Locale.ROOT)))
                        .first();
            }
            return doc;
        } catch (Exception e) {
            LOGGER.error("Erro ao buscar documento no MongoDB para {}: {}", player.getName(), e.getMessage());
            return null;
        }
    }

    private static Rank parseRank(String name) {
        try {
            return Rank.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Rank.DEFAULT;
        }
    }
}
