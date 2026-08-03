package com.ascend.core.spigot.tab;

import com.ascend.core.api.nms.NMSHelper;
import org.bukkit.entity.Player;

public class TabHeaderFooter {

    private static final String HEADER = "\n&b&lASCEND\n";
    private static final String FOOTER = "\n&eWebsite &bascendstudios.net\n&eLoja &bloja.ascendstudios.net\n&eDiscord &bdiscord.gg/ascend\n";

    public static void send(Player player) {
        if (player == null) return;
        String header = NMSHelper.color(HEADER);
        String footer = NMSHelper.color(FOOTER);

        try {
            Class<?> packetClass = NMSHelper.getNMSClass("PacketPlayOutPlayerListHeaderFooter");

            Object h = NMSHelper.createChatComponent(header);
            Object f = NMSHelper.createChatComponent(footer);

            Object packet = packetClass.getConstructor().newInstance();
            NMSHelper.setDeclaredField(packet, "a", h);
            NMSHelper.setDeclaredField(packet, "b", f);

            NMSHelper.sendPacket(player, packet);
        } catch (Throwable t) {
            try {
                player.getClass().getMethod("setPlayerListHeaderFooter", String.class, String.class)
                      .invoke(player, header, footer);
            } catch (Throwable ignored) {}
        }
    }
}
