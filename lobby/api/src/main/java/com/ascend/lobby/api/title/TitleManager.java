package com.ascend.lobby.api.title;

import com.ascend.core.api.nms.NMSHelper;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TitleManager {

    private static final List<String> SUBTITLES = List.of(
            "&eBoas vindas!",
            "&e100% open source!",
            "&eQuer um café?"
    );

    private TitleManager() {}

    public static void sendRandomWelcomeTitle(Player player) {
        String randomSubtitle = SUBTITLES.get(ThreadLocalRandom.current().nextInt(SUBTITLES.size()));
        sendTitle(player, "&b&lASCEND", randomSubtitle, 10, 40, 10);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) return;
        try {
            Class<?> packetTitleClass = NMSHelper.getNMSClass("PacketPlayOutTitle");
            Class<?> enumTitleActionClass = NMSHelper.getNMSClass("PacketPlayOutTitle$EnumTitleAction");
            Class<?> chatComponentClass = NMSHelper.getNMSClass("IChatBaseComponent");

            Object timesPacket = packetTitleClass.getConstructor(int.class, int.class, int.class).newInstance(fadeIn, stay, fadeOut);
            NMSHelper.sendPacket(player, timesPacket);

            if (title != null) {
                Object tSerialized = NMSHelper.createChatComponent(title);
                Object tEnum = enumTitleActionClass.getField("TITLE").get(null);
                Object tPacket = packetTitleClass.getConstructor(enumTitleActionClass, chatComponentClass).newInstance(tEnum, tSerialized);
                NMSHelper.sendPacket(player, tPacket);
            }

            if (subtitle != null) {
                Object sSerialized = NMSHelper.createChatComponent(subtitle);
                Object sEnum = enumTitleActionClass.getField("SUBTITLE").get(null);
                Object sPacket = packetTitleClass.getConstructor(enumTitleActionClass, chatComponentClass).newInstance(sEnum, sSerialized);
                NMSHelper.sendPacket(player, sPacket);
            }
        } catch (Throwable t) {
            try {
                player.sendTitle(NMSHelper.color(title), NMSHelper.color(subtitle));
            } catch (Throwable ignored) {}
        }
    }
}
