package com.ascend.core.velocity.command;

import com.ascend.core.api.backend.mongo.Connect;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.rank.Rank;
import com.ascend.core.api.tag.Tag;
import com.ascend.core.velocity.CoreVelocityPlugin;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bson.Document;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class StaffChatCommand implements SimpleCommand {

    private static final Set<UUID> TOGGLED_STAFF = new HashSet<>();
    private final CoreVelocityPlugin plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();

    public StaffChatCommand(CoreVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isToggled(UUID uuid) {
        return TOGGLED_STAFF.contains(uuid);
    }

    public boolean hasStaffPermission(CommandSource source) {
        Rank rank = plugin.getPlayerRank(source);
        return rank.isStaff();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!hasStaffPermission(source)) {
            source.sendMessage(serializer.deserialize("§cVocê não possui permissão para utilizar este comando."));
            return;
        }

        if (args.length == 0) {
            if (source instanceof Player) {
                Player player = (Player) source;
                UUID uuid = player.getUniqueId();
                if (TOGGLED_STAFF.contains(uuid)) {
                    TOGGLED_STAFF.remove(uuid);
                    player.sendMessage(serializer.deserialize("§cStaffChat desativado."));
                } else {
                    TOGGLED_STAFF.add(uuid);
                    player.sendMessage(serializer.deserialize("§aSuas mensagens serão enviadas no StaffChat."));
                }
            } else {
                source.sendMessage(serializer.deserialize("§cUso correto: /sc <mensagem>"));
            }
            return;
        }

        String message = String.join(" ", args);
        sendStaffMessage(source, message);
    }

    public void sendStaffMessage(CommandSource sender, String messageText) {
        String tagPrefix = getTagPrefix(sender);
        String name = sender instanceof Player ? ((Player) sender).getUsername() : "Console";
        String formatted = "§d§l[STAFF] " + tagPrefix + name + " §8» §f" + messageText;

        plugin.getServer().getConsoleCommandSource().sendMessage(serializer.deserialize(formatted));

        for (Player recipient : plugin.getServer().getAllPlayers()) {
            if (hasStaffPermission(recipient)) {
                recipient.sendMessage(serializer.deserialize(formatted));
            }
        }
    }

    private String getTagPrefix(CommandSource source) {
        if (!(source instanceof Player)) {
            return Tag.ADMIN.getPrefix();
        }

        Player p = (Player) source;
        String usernameLower = p.getUsername().toLowerCase(Locale.ROOT);

        // 1. Tenta carregar Tag do Redis (L1 Cache)
        RedisConnect redis = plugin.getRedisConnection();
        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                String tagName = jedis.hget("player:" + usernameLower, "tag");
                if (tagName != null) {
                    Tag tag = Tag.fromName(tagName);
                    if (tag != null) return tag.getPrefix();
                }
            } catch (Exception ignored) {}
        }

        // 2. Fallback MongoDB (L2)
        try {
            Connect mongo = plugin.getMongoConnection();
            if (mongo != null && mongo.isConnected()) {
                MongoCollection<Document> collection = mongo.getDatabase().getCollection("accounts");
                Document doc = collection.find(Filters.eq("username_lower", usernameLower)).first();
                if (doc != null && doc.containsKey("tag")) {
                    Tag tag = Tag.fromName(doc.getString("tag"));
                    if (tag != null) return tag.getPrefix();
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback pelo Rank do jogador
        Rank rank = plugin.getPlayerRank(source);
        return Tag.fromRank(rank).getPrefix();
    }
}
