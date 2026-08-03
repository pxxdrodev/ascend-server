package com.ascend.core.api.account;

import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.rank.Rank;
import com.ascend.core.api.tag.Tag;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AccountCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountCache.class);
    private static final Map<UUID, Account> ACCOUNTS_BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, Account> ACCOUNTS_BY_NAME = new ConcurrentHashMap<>();

    private AccountCache() {}

    public static Account getAccount(UUID uuid) {
        if (uuid == null) return null;
        return ACCOUNTS_BY_UUID.get(uuid);
    }

    public static Account getAccount(String username) {
        if (username == null || username.isBlank()) return null;
        return ACCOUNTS_BY_NAME.get(username.toLowerCase(Locale.ROOT));
    }

    public static Account loadAccount(UUID uuid, String username, RedisConnect redis, Connect mongo) {
        if (username == null || username.isBlank()) return null;
        String userLower = username.toLowerCase(Locale.ROOT);
        String uuidStr = uuid != null ? uuid.toString() : null;

        Rank rank = Rank.DEFAULT;
        Tag tag = Tag.MEMBRO;
        int level = 1;

        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                String key = (uuidStr != null) ? "player:uuid:" + uuidStr : "player:name:" + userLower;
                Map<String, String> map = jedis.hgetAll(key);
                if (map == null || map.isEmpty()) {
                    map = jedis.hgetAll("player:" + userLower);
                }

                if (map != null && !map.isEmpty()) {
                    if (map.containsKey("rank")) {
                        try { rank = Rank.valueOf(map.get("rank").toUpperCase(Locale.ROOT)); } catch (Exception e) {
                            LOGGER.warn("Falha ao analisar rank do Redis para '{}': {}", username, e.getMessage());
                        }
                    }
                    if (map.containsKey("tag")) {
                        Tag t = Tag.fromName(map.get("tag"));
                        if (t != null) tag = t;
                    }
                    if (map.containsKey("level")) {
                        try { level = Integer.parseInt(map.get("level")); } catch (Exception e) {
                            LOGGER.warn("Falha ao analisar level do Redis para '{}': {}", username, e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Erro ao carregar dados do Redis para '{}' (UUID: {}): {}", username, uuidStr, e.getMessage());
            }
        }

        if (rank == Rank.DEFAULT && mongo != null && mongo.isConnected()) {
            try {
                MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                Document doc = null;

                if (uuidStr != null) {
                    doc = col.find(Filters.eq("uuid", uuidStr)).first();
                }
                if (doc == null) {
                    doc = col.find(Filters.eq("username_lower", userLower)).first();
                }

                if (doc != null) {
                    if (doc.containsKey("rank")) {
                        try { rank = Rank.valueOf(doc.getString("rank").toUpperCase(Locale.ROOT)); } catch (Exception e) {
                            LOGGER.warn("Falha ao analisar rank do MongoDB para '{}': {}", username, e.getMessage());
                        }
                    }
                    if (doc.containsKey("tag")) {
                        Tag t = Tag.fromName(doc.getString("tag"));
                        if (t != null) tag = t;
                    }
                    if (doc.containsKey("level")) {
                        level = doc.getInteger("level", 1);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("ERRO CRÍTICO MONGODB: Falha ao carregar conta do jogador '{}' (UUID: {}). Verifique se o banco está online!", username, uuidStr, e);
            }
        }

        if ((redis == null || !redis.isConnected()) && (mongo == null || !mongo.isConnected())) {
            LOGGER.error("ALERTA BANCOS DESCONECTADOS: Não foi possível verificar dados do jogador '{}' (UUID: {}). Atribuído rank padrão temporário!", username, uuidStr);
        }

        if (tag != Tag.MEMBRO && tag.getRank().ordinal() < rank.ordinal()) {
            tag = Tag.fromRank(rank);
        } else if (tag == Tag.MEMBRO && rank != Rank.DEFAULT) {
            tag = Tag.fromRank(rank);
        }

        Account account = new Account(uuid, username, rank, tag, level);
        if (uuid != null) ACCOUNTS_BY_UUID.put(uuid, account);
        ACCOUNTS_BY_NAME.put(userLower, account);
        return account;
    }

    public static void cacheAccount(Account account) {
        if (account == null) return;
        if (account.getUniqueId() != null) {
            ACCOUNTS_BY_UUID.put(account.getUniqueId(), account);
        }
        if (account.getUsername() != null) {
            ACCOUNTS_BY_NAME.put(account.getUsername().toLowerCase(Locale.ROOT), account);
        }
    }

    public static void invalidate(UUID uuid, String username) {
        if (uuid != null) ACCOUNTS_BY_UUID.remove(uuid);
        if (username != null) ACCOUNTS_BY_NAME.remove(username.toLowerCase(Locale.ROOT));
    }
}
