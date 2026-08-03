package com.ascend.core.velocity.command;

import com.ascend.core.api.account.Account;
import com.ascend.core.api.account.AccountCache;
import com.ascend.core.api.rank.Rank;
import com.ascend.core.api.tag.Tag;
import com.ascend.core.velocity.CoreVelocityPlugin;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        if (!(source instanceof Player)) return true;
        Player player = (Player) source;
        Account account = AccountCache.getAccount(player.getUniqueId());
        if (account != null) {
            return account.isStaff();
        }
        Rank rank = plugin.getPlayerRank(source);
        return rank.isStaff();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!hasStaffPermission(source)) {
            source.sendMessage(serializer.deserialize("§cVocê não tem permissão para executar este comando."));
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
                source.sendMessage(serializer.deserialize("§cUso correto: /sc <on|off|mensagem>"));
            }
            return;
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("on")) {
                if (source instanceof Player) {
                    Player player = (Player) source;
                    TOGGLED_STAFF.add(player.getUniqueId());
                    player.sendMessage(serializer.deserialize("§aSuas mensagens serão enviadas no StaffChat."));
                }
                return;
            } else if (args[0].equalsIgnoreCase("off")) {
                if (source instanceof Player) {
                    Player player = (Player) source;
                    TOGGLED_STAFF.remove(player.getUniqueId());
                    player.sendMessage(serializer.deserialize("§cStaffChat desativado."));
                }
                return;
            }
        }

        String message = String.join(" ", args);
        sendStaffMessage(source, message);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (!hasStaffPermission(invocation.source())) {
            return List.of();
        }
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            String input = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            return List.of("on", "off").stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }
        return List.of();
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
        Account account = AccountCache.getAccount(p.getUniqueId());
        if (account != null && account.getTag() != null) {
            return account.getTag().getPrefix();
        }

        Rank rank = plugin.getPlayerRank(source);
        return Tag.fromRank(rank).getPrefix();
    }
}
