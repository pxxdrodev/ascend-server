package com.ascend.core.velocity.command;

import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.rank.Rank;
import com.ascend.core.velocity.CoreVelocityPlugin;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bson.Document;
import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class GroupCommand implements SimpleCommand {

    private final CoreVelocityPlugin plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public GroupCommand(CoreVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!canExecuteGroupCommand(source)) {
            source.sendMessage(serializer.deserialize("§cVocê não possui permissão para executar este comando."));
            return;
        }

        String targetName = null;
        String groupName = null;

        if (args.length >= 3 && args[0].equalsIgnoreCase("set")) {
            targetName = args[1];
            groupName = args[2];
        } else if (args.length >= 3 && args[1].equalsIgnoreCase("set")) {
            targetName = args[0];
            groupName = args[2];
        }

        if (targetName == null || groupName == null) {
            source.sendMessage(serializer.deserialize("§cUso correto: /group set <nick> <grupo>"));
            return;
        }

        Rank targetRank = parseRank(groupName);
        if (targetRank == null) {
            String available = Arrays.stream(Rank.values())
                    .map(Rank::getColoredName)
                    .collect(Collectors.joining("§f, "));
            source.sendMessage(serializer.deserialize("§cGrupo '" + groupName + "' não encontrado. Grupos disponíveis: " + available));
            return;
        }

        if (source instanceof Player) {
            Player sender = (Player) source;

            if (sender.getUsername().equalsIgnoreCase(targetName)) {
                source.sendMessage(serializer.deserialize("§cVocê não pode alterar o seu próprio grupo."));
                return;
            }

            Rank senderRank = plugin.getPlayerRank(source);
            if (senderRank != Rank.ADMIN && targetRank.ordinal() <= senderRank.ordinal()) {
                source.sendMessage(serializer.deserialize("§cVocê não pode alterar para um grupo igual ou superior ao seu."));
                return;
            }
        }

        final String finalTargetName = targetName;
        final String targetLower = targetName.toLowerCase(Locale.ROOT);
        final Rank finalTargetRank = targetRank;

        CompletableFuture.runAsync(() -> {
            // 1. Redis L1 Cache
            RedisConnect redis = plugin.getRedisConnection();
            if (redis != null && redis.isConnected()) {
                try (Jedis jedis = redis.getResource()) {
                    jedis.hset("player:" + targetLower, "rank", finalTargetRank.name());
                } catch (Exception e) {
                    plugin.getLogger().warn("Falha ao atualizar rank no Redis para {}: {}", finalTargetName, e.getMessage());
                }
            }

            // 2. MongoDB L2 Persistence
            Connect mongo = plugin.getMongoConnection();
            if (mongo != null && mongo.isConnected()) {
                try {
                    MongoCollection<Document> collection = mongo.getDatabase().getCollection("accounts");
                    collection.updateOne(
                            Filters.eq("username_lower", targetLower),
                            Updates.combine(
                                    Updates.set("username", finalTargetName),
                                    Updates.set("username_lower", targetLower),
                                    Updates.set("rank", finalTargetRank.name())
                            ),
                            new UpdateOptions().upsert(true)
                    );
                } catch (Exception e) {
                    plugin.getLogger().warn("Falha ao salvar rank no MongoDB para {}: {}", finalTargetName, e.getMessage());
                }
            }
        });

        source.sendMessage(serializer.deserialize("§aO grupo do jogador §f" + targetName + " §afoi alterado para " + targetRank.getColoredName() + "§a."));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            return List.of("set");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return plugin.getServer().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return Arrays.stream(Rank.values())
                    .map(Rank::name)
                    .map(String::toLowerCase)
                    .filter(name -> name.startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private Rank parseRank(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        if (input.equalsIgnoreCase("membro") || input.equalsIgnoreCase("default")) {
            return Rank.DEFAULT;
        }
        if (input.equalsIgnoreCase("mod+") || input.equalsIgnoreCase("modplus")) {
            return Rank.MOD_PLUS;
        }
        if (input.equalsIgnoreCase("plus") || input.equalsIgnoreCase("partnerplus") || input.equalsIgnoreCase("partner+")) {
            return Rank.PARTNER_PLUS;
        }

        String clean = input.toUpperCase(Locale.ROOT).replace("+", "_PLUS");
        for (Rank rank : Rank.values()) {
            if (rank.name().equalsIgnoreCase(clean) || rank.getDisplayName().equalsIgnoreCase(input)) {
                return rank;
            }
        }
        return null;
    }

    private boolean canExecuteGroupCommand(CommandSource source) {
        Rank rank = plugin.getPlayerRank(source);
        return rank == Rank.ADMIN || rank == Rank.MOD_PLUS || rank == Rank.MOD;
    }
}
