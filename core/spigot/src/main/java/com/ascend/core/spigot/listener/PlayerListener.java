package com.ascend.core.spigot.listener;

import com.ascend.core.api.account.Account;
import com.ascend.core.api.account.AccountCache;
import com.ascend.core.api.rank.Rank;
import com.ascend.core.api.tag.Tag;
import com.ascend.core.spigot.CoreSpigotPlugin;
import com.ascend.core.spigot.tag.TagManager;
import com.ascend.core.spigot.tab.TabHeaderFooter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private static final Map<UUID, PermissionAttachment> ATTACHMENTS = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Account account = AccountCache.loadAccount(
                player.getUniqueId(),
                player.getName(),
                CoreSpigotPlugin.getInstance().getRedisConnection(),
                CoreSpigotPlugin.getInstance().getMongoConnection()
        );

        Rank rank = account != null ? account.getRank() : TagManager.getPlayerRank(player);
        Tag tag = account != null ? account.getTag() : TagManager.getPlayerTag(player);
        if (tag == Tag.MEMBRO) tag = Tag.fromRank(rank);

        updatePlayerPermissions(player, rank);
        TagManager.applyTag(player, tag);
        TabHeaderFooter.send(player);
    }

    public static void updatePlayerPermissions(Player player, Rank rank) {
        if (player == null || !player.isOnline()) return;

        PermissionAttachment oldAttachment = ATTACHMENTS.remove(player.getUniqueId());
        if (oldAttachment != null) {
            try {
                player.removeAttachment(oldAttachment);
            } catch (Exception ignored) {}
        }

        PermissionAttachment attachment = player.addAttachment(CoreSpigotPlugin.getInstance());
        ATTACHMENTS.put(player.getUniqueId(), attachment);

        for (Rank r : Rank.values()) {
            if (r.ordinal() >= rank.ordinal() && !r.getPermission().isEmpty()) {
                attachment.setPermission(r.getPermission(), true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        AccountCache.invalidate(player.getUniqueId(), player.getName());

        PermissionAttachment att = ATTACHMENTS.remove(player.getUniqueId());
        if (att != null) {
            try { player.removeAttachment(att); } catch (Exception ignored) {}
        }
    }
}
