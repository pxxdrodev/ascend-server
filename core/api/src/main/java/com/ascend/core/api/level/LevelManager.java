package com.ascend.core.api.level;

import com.ascend.core.api.account.Account;
import com.ascend.core.api.account.AccountCache;
import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LevelManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LevelManager.class);

    private LevelManager() {}

    public static int getRequiredXp(int level) {
        return level * 1000;
    }

    public static int getLevel(String username, RedisConnect redis, Connect mongo) {
        if (username == null || username.isBlank()) return 1;

        Account account = AccountCache.getAccount(username);
        if (account != null && account.getLevel() > 0) {
            return account.getLevel();
        }

        String userLower = username.toLowerCase(Locale.ROOT);

        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                String key = "player:" + userLower;
                String lvl = jedis.hget(key, "level");
                if (lvl != null) {
                    try { return Integer.parseInt(lvl); } catch (NumberFormatException ignored) {}
                }
            } catch (Exception e) {
                LOGGER.warn("Erro ao buscar nível no Redis para {}: {}", username, e.getMessage());
            }
        }

        if (mongo != null && mongo.isConnected()) {
            try {
                MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                Document doc = col.find(Filters.eq("username_lower", userLower)).first();
                if (doc != null && doc.containsKey("level")) {
                    return doc.getInteger("level", 1);
                }
            } catch (Exception e) {
                LOGGER.error("Erro ao carregar nível do MongoDB para {}: {}", username, e.getMessage());
            }
        }

        return 1;
    }

    public static boolean addXp(UUID uniqueId, String username, int xpGained, RedisConnect redis, Connect mongo) {
        if (username == null || username.isBlank() || xpGained <= 0) return false;

        String userLower = username.toLowerCase(Locale.ROOT);
        int currentLevel = getLevel(username, redis, mongo);
        int currentXp = getXp(username, redis, mongo) + xpGained;
        boolean leveledUp = false;

        while (currentXp >= getRequiredXp(currentLevel)) {
            currentXp -= getRequiredXp(currentLevel);
            currentLevel++;
            leveledUp = true;
        }

        saveLevelAndXp(uniqueId, username, currentLevel, currentXp, redis, mongo);
        return leveledUp;
    }

    public static int getXp(String username, RedisConnect redis, Connect mongo) {
        String userLower = username.toLowerCase(Locale.ROOT);

        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                String key = "player:" + userLower;
                String xp = jedis.hget(key, "xp");
                if (xp != null) {
                    try { return Integer.parseInt(xp); } catch (NumberFormatException ignored) {}
                }
            } catch (Exception ignored) {}
        }

        if (mongo != null && mongo.isConnected()) {
            try {
                MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                Document doc = col.find(Filters.eq("username_lower", userLower)).first();
                if (doc != null && doc.containsKey("xp")) {
                    return doc.getInteger("xp", 0);
                }
            } catch (Exception ignored) {}
        }

        return 0;
    }

    public static void saveLevel(String username, int level, RedisConnect redis, Connect mongo) {
        saveLevelAndXp(null, username, level, getXp(username, redis, mongo), redis, mongo);
    }

    public static void saveLevelAndXp(UUID uniqueId, String username, int level, int xp, RedisConnect redis, Connect mongo) {
        if (username == null || username.isBlank()) return;

        Account account = AccountCache.getAccount(username);
        if (account != null) {
            account.setLevel(level);
        }

        String userLower = username.toLowerCase(Locale.ROOT);

        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                String key = "player:" + userLower;
                jedis.hset(key, "level", String.valueOf(level));
                jedis.hset(key, "xp", String.valueOf(xp));
                if (uniqueId != null) {
                    jedis.hset("player:uuid:" + uniqueId.toString(), "level", String.valueOf(level));
                    jedis.hset("player:uuid:" + uniqueId.toString(), "xp", String.valueOf(xp));
                }
            } catch (Exception e) {
                LOGGER.warn("Erro ao salvar nível e XP no Redis para {}: {}", username, e.getMessage());
            }
        }

        CompletableFuture.runAsync(() -> {
            if (mongo == null || !mongo.isConnected()) {
                LOGGER.warn("MongoDB desconectado! Impossível persistir nível de {} no MongoDB.", username);
                return;
            }

            try {
                MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                col.updateOne(
                        Filters.eq("username_lower", userLower),
                        Updates.combine(
                                Updates.set("username", username),
                                Updates.set("username_lower", userLower),
                                Updates.set("level", level),
                                Updates.set("xp", xp)
                        ),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception e) {
                LOGGER.error("Erro ao salvar nível no MongoDB para {}: {}", username, e.getMessage());
            }
        });
    }
}
