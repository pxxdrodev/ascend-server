package com.ascend.game.spigot.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SoupListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSoup(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.MUSHROOM_SOUP) return;

        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();

        if (health < maxHealth) {
            event.setCancelled(true);
            player.setHealth(Math.min(health + 7.0, maxHealth));
            player.setItemInHand(new ItemStack(Material.BOWL));
        } else if (player.getFoodLevel() < 20) {
            event.setCancelled(true);
            player.setFoodLevel(Math.min(player.getFoodLevel() + 6, 20));
            player.setItemInHand(new ItemStack(Material.BOWL));
        }
    }
}
