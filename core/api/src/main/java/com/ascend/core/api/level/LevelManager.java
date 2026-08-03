package com.ascend.core.api.level;

import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import redis.clients.jedis.Jedis;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class LevelManager {

    private static final String KEY_PREFIX = "player:";

    private LevelManager() {}

    public static int getLevel(String username, RedisConnect redis, Connect mongo) {
        if (username == null || username.isBlank()) return 1;
        String userLower = username.toLowerCase(Locale.ROOT);
        String key = KEY_PREFIX + userLower;

        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                String lvlStr = jedis.hget(key, "level");
                if (lvlStr != null) {
                    try {
                        return Integer.parseInt(lvlStr);
                    } catch (NumberFormatException ignored) {}
                }
            } catch (Exception ignored) {}
        }

        if (mongo != null && mongo.isConnected()) {
            try {
                MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                Document doc = col.find(Filters.eq("username_lower", userLower)).first();
                if (doc != null && doc.containsKey("level")) {
                    int level = doc.getInteger("level", 1);
                    if (redis != null && redis.isConnected()) {
                        try (Jedis jedis = redis.getResource()) {
                            jedis.hset(key, "level", String.valueOf(level));
                        } catch (Exception ignored) {}
                    }
                    return level;
                }
            } catch (Exception ignored) {}
        }

        return 1; 
    }

    public static void saveLevel(String username, int level, RedisConnect redis, Connect mongo) {
        if (username == null || username.isBlank()) return;
        String userLower = username.toLowerCase(Locale.ROOT);
        String key = KEY_PREFIX + userLower;

        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                jedis.hset(key, "level", String.valueOf(level));
            } catch (Exception ignored) {}
        }

        CompletableFuture.runAsync(() -> {
            if (mongo != null && mongo.isConnected()) {
                try {
                    MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                    col.updateOne(
                            Filters.eq("username_lower", userLower),
                            Updates.set("level", level),
                            new UpdateOptions().upsert(true)
                    );
                } catch (Exception ignored) {}
            }
        });
    }
}
