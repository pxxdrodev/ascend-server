package com.ascend.core.spigot.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.ascend.core.api.rank.Rank;
import com.ascend.core.api.tag.Tag;
import com.ascend.core.spigot.tag.TagManager;
import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CommandAlias("tag|tags")
public class TagCommand extends BaseCommand {

    @Default
    @Subcommand("list")
    public void onListTags(Player player) {
        List<Tag> accessible = new ArrayList<>();
        for (Tag tag : Tag.values()) {
            if (hasAccessToTag(player, tag)) accessible.add(tag);
        }

        TextComponent msg = new TextComponent(color("&aSuas tags: "));
        for (int i = 0; i < accessible.size(); i++) {
            Tag tag = accessible.get(i);
            String cmd = "/tag " + tag.name().toLowerCase(Locale.ROOT).replace("_plus", "+");

            TextComponent comp = new TextComponent(tag.getNameFormattedColored());
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(color("&eClique para selecionar: " + tag.getNameFormattedColored())).create()));
            msg.addExtra(comp);
            if (i < accessible.size() - 1) msg.addExtra(new TextComponent(color("&f, ")));
        }
        player.spigot().sendMessage(msg);
    }

    @Default
    @Syntax("<tag>")
    public void onSelectTag(Player player, Tag tag) {
        if (!hasAccessToTag(player, tag)) {
            player.sendMessage(color("&cEsta tag não foi encontrada ou você não a possui."));
            return;
        }

        // Salva no Redis (L1) + MongoDB (L2) e aplica a tag/hierarquia no Tab
        TagManager.saveTag(player, tag);
        TagManager.applyTag(player, tag);
        player.sendMessage(color("&aSua tag foi alterada para: " + tag.getNameFormattedColored()));
    }

    public static boolean hasAccessToTag(Player player, Tag tag) {
        if (player.isOp()) return true;
        Rank tagRank = tag.getRank();
        if (tagRank == Rank.DEFAULT || tagRank.getPermission().isEmpty()) return true;

        for (Rank r : Rank.values()) {
            if (r.ordinal() <= tagRank.ordinal() &&
                !r.getPermission().isEmpty() &&
                player.hasPermission(r.getPermission())) {
                return true;
            }
        }
        return false;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
