package com.ascend.core.spigot.tag;

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
import redis.clients.jedis.Jedis;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class TagManager {

    private static final String KEY_PREFIX = "player:";

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
            player.setDisplayName(tag.getPrefixColored() + player.getName() + ChatColor.RESET);
        }
    }


    public static Rank getPlayerRank(Player player) {
        String key = key(player);
        String rankName = redisGet(key, "rank");

        if (rankName != null) {
            return parseRank(rankName);
        }

        Document doc = findDocument(player);
        if (doc != null && doc.containsKey("rank")) {
            rankName = doc.getString("rank");
            redisSave(key, "rank", rankName);
            return parseRank(rankName);
        }
        return Rank.DEFAULT;
    }

    public static Tag getPlayerTag(Player player) {
        String key = key(player);
        String tagName = redisGet(key, "tag");

        if (tagName != null) {
            Tag tag = Tag.fromName(tagName);
            return tag != null ? tag : Tag.MEMBRO;
        }

        Document doc = findDocument(player);
        if (doc != null && doc.containsKey("tag")) {
            tagName = doc.getString("tag");
            redisSave(key, "tag", tagName);
            Tag tag = Tag.fromName(tagName);
            return tag != null ? tag : Tag.MEMBRO;
        }
        return Tag.MEMBRO;
    }

    public static void saveTag(Player player, Tag tag) {
        String key = key(player);
        String tagName = tag.name();

        redisSave(key, "tag", tagName);

        CompletableFuture.runAsync(() -> {
            try {
                Connect mongo = CoreSpigotPlugin.getInstance().getMongoConnection();
                if (mongo == null || !mongo.isConnected()) return;
                MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                col.updateOne(
                        Filters.eq("username_lower", player.getName().toLowerCase(Locale.ROOT)),
                        Updates.set("tag", tagName),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception ignored) {}
        });
    }

    public static void saveRank(Player player, Rank rank) {
        String key = key(player);
        String rankName = rank.name();

        redisSave(key, "rank", rankName);

        CompletableFuture.runAsync(() -> {
            try {
                Connect mongo = CoreSpigotPlugin.getInstance().getMongoConnection();
                if (mongo == null || !mongo.isConnected()) return;
                mongo.getDatabase().getCollection("accounts").updateOne(
                        Filters.eq("username_lower", player.getName().toLowerCase(Locale.ROOT)),
                        Updates.set("rank", rankName),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception ignored) {}
        });
    }

    public static void evict(Player player) {
        RedisConnect redis = CoreSpigotPlugin.getInstance().getRedisConnection();
        if (redis == null || !redis.isConnected()) return;
        try (Jedis jedis = redis.getResource()) {
            jedis.del(key(player));
        } catch (Exception ignored) {}
    }

    private static String key(Player player) {
        return KEY_PREFIX + player.getName().toLowerCase(Locale.ROOT);
    }

    private static String redisGet(String key, String field) {
        RedisConnect redis = CoreSpigotPlugin.getInstance().getRedisConnection();
        if (redis == null || !redis.isConnected()) return null;
        try (Jedis jedis = redis.getResource()) {
            return jedis.hget(key, field);
        } catch (Exception e) {
            return null;
        }
    }

    private static void redisSave(String key, String field, String value) {
        RedisConnect redis = CoreSpigotPlugin.getInstance().getRedisConnection();
        if (redis == null || !redis.isConnected()) return;
        try (Jedis jedis = redis.getResource()) {
            jedis.hset(key, field, value);
        } catch (Exception ignored) {}
    }

    private static Document findDocument(Player player) {
        Connect mongo = CoreSpigotPlugin.getInstance().getMongoConnection();
        if (mongo == null || !mongo.isConnected()) return null;
        try {
            return mongo.getDatabase()
                        .getCollection("accounts")
                        .find(Filters.eq("username_lower", player.getName().toLowerCase(Locale.ROOT)))
                        .first();
        } catch (Exception e) {
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
