package com.ascend.core.velocity.utils;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ServerMotd {

    private static final Component MOTD = LegacyComponentSerializer.legacyAmpersand().deserialize(
            "                 &r   &aAscend Network &c[1.8/1.7]\n" +
            "                &b&lNOVA NETWORK INOVADORA!"
    );

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing.Builder builder = event.getPing().asBuilder();
        builder.description(MOTD);
        event.setPing(builder.build());
    }
}
