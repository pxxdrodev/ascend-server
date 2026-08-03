package com.ascend.game.api.stats;

import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import redis.clients.jedis.Jedis;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class InfectionSoupStats {

    private final UUID uniqueId;
    private final String username;

    private int wins;
    private int losses;
    private int humansSurvived;
    private int humansInfected;
    private int infectedKilled;
    private int coins;

    public InfectionSoupStats(UUID uniqueId, String username) {
        this.uniqueId = uniqueId;
        this.username = username;
    }

    public static InfectionSoupStats load(UUID uniqueId, String username, RedisConnect redis, Connect mongo) {
        InfectionSoupStats stats = new InfectionSoupStats(uniqueId, username);
        if (username == null) return stats;

        String userLower = username.toLowerCase(Locale.ROOT);

        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                String key = "player:" + userLower;
                String w = jedis.hget(key, "inf_wins");
                if (w != null) {
                    try { stats.wins = Integer.parseInt(w); } catch (Exception ignored) {}
                    try { stats.losses = Integer.parseInt(jedis.hget(key, "inf_losses")); } catch (Exception ignored) {}
                    try { stats.humansSurvived = Integer.parseInt(jedis.hget(key, "inf_humans_survived")); } catch (Exception ignored) {}
                    try { stats.humansInfected = Integer.parseInt(jedis.hget(key, "inf_humans_infected")); } catch (Exception ignored) {}
                    try { stats.infectedKilled = Integer.parseInt(jedis.hget(key, "inf_infected_killed")); } catch (Exception ignored) {}
                    try { stats.coins = Integer.parseInt(jedis.hget(key, "coins")); } catch (Exception ignored) {}
                    return stats;
                }
            } catch (Exception ignored) {}
        }

        if (mongo != null && mongo.isConnected()) {
            try {
                MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                Document doc = col.find(Filters.eq("username_lower", userLower)).first();
                if (doc != null) {
                    stats.wins = doc.getInteger("inf_wins", 0);
                    stats.losses = doc.getInteger("inf_losses", 0);
                    stats.humansSurvived = doc.getInteger("inf_humans_survived", 0);
                    stats.humansInfected = doc.getInteger("inf_humans_infected", 0);
                    stats.infectedKilled = doc.getInteger("inf_infected_killed", 0);
                    stats.coins = doc.getInteger("coins", 0);
                }
            } catch (Exception ignored) {}
        }

        return stats;
    }

    public void save(RedisConnect redis, Connect mongo) {
        if (username == null) return;
        String userLower = username.toLowerCase(Locale.ROOT);

        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                String key = "player:" + userLower;
                jedis.hset(key, "inf_wins", String.valueOf(wins));
                jedis.hset(key, "inf_losses", String.valueOf(losses));
                jedis.hset(key, "inf_humans_survived", String.valueOf(humansSurvived));
                jedis.hset(key, "inf_humans_infected", String.valueOf(humansInfected));
                jedis.hset(key, "inf_infected_killed", String.valueOf(infectedKilled));
                jedis.hset(key, "coins", String.valueOf(coins));
                if (uniqueId != null) {
                    String uuidKey = "player:uuid:" + uniqueId.toString();
                    jedis.hset(uuidKey, "coins", String.valueOf(coins));
                }
            } catch (Exception ignored) {}
        }

        CompletableFuture.runAsync(() -> {
            if (mongo == null || !mongo.isConnected()) return;
            try {
                MongoCollection<Document> col = mongo.getDatabase().getCollection("accounts");
                col.updateOne(
                        Filters.eq("username_lower", userLower),
                        Updates.combine(
                                Updates.set("username", username),
                                Updates.set("username_lower", userLower),
                                Updates.set("inf_wins", wins),
                                Updates.set("inf_losses", losses),
                                Updates.set("inf_humans_survived", humansSurvived),
                                Updates.set("inf_humans_infected", humansInfected),
                                Updates.set("inf_infected_killed", infectedKilled),
                                Updates.set("coins", coins)
                        ),
                        new UpdateOptions().upsert(true)
                );
            } catch (Exception ignored) {}
        });
    }
}
