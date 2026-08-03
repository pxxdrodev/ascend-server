package com.ascend.core.velocity.listener;

import com.ascend.core.velocity.CoreVelocityPlugin;
import com.ascend.core.velocity.command.StaffChatCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;

public class StaffChatListener {

    private final CoreVelocityPlugin plugin;
    private final StaffChatCommand staffChatCommand;

    public StaffChatListener(CoreVelocityPlugin plugin, StaffChatCommand staffChatCommand) {
        this.plugin = plugin;
        this.staffChatCommand = staffChatCommand;
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        if (StaffChatCommand.isToggled(event.getPlayer().getUniqueId())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            staffChatCommand.sendStaffMessage(event.getPlayer(), event.getMessage());
        }
    }
}
