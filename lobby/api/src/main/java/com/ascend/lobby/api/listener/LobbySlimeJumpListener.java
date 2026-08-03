package com.ascend.lobby.api.listener;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class LobbySlimeJumpListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        Block block = loc.getBlock();
        Block under = block.getRelative(BlockFace.DOWN);

        boolean isSlime = block.getType() == Material.SLIME_BLOCK || under.getType() == Material.SLIME_BLOCK;

        if (!isSlime) {
            Block under2 = under.getRelative(BlockFace.DOWN);
            if (under2.getType() == Material.SLIME_BLOCK) {
                isSlime = true;
            }
        }

        if (isSlime) {
            Vector velocity = loc.getDirection().multiply(1.8D).setY(1.2D);
            player.setVelocity(velocity);

            try {
                player.playSound(loc, Sound.FIREWORK_LAUNCH, 1.0F, 1.2F);
            } catch (Exception ignored) {}

            try {
                player.getWorld().playEffect(loc, Effect.MOBSPAWNER_FLAMES, 0);
            } catch (Exception ignored) {}
        }
    }
}
