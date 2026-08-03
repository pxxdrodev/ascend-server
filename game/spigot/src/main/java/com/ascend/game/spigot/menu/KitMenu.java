package com.ascend.game.spigot.menu;

import com.ascend.game.api.InfectionSoupAPI;
import com.ascend.game.api.kit.HumanKit;
import com.ascend.game.api.kit.InfectedKit;
import com.ascend.game.api.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class KitMenu implements Listener {

    public static void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Selecionar Kits");

        GamePlayer gp = InfectionSoupAPI.getInstance().getPlayer(player.getUniqueId());
        HumanKit selectedHuman = gp != null ? gp.getHumanKit() : HumanKit.SOLDADO;
        InfectedKit selectedInfected = gp != null ? gp.getInfectedKit() : InfectedKit.RUNNER;

        int[] humanSlots = {10, 11, 12, 13, 14};
        HumanKit[] humanKits = HumanKit.values();
        for (int i = 0; i < humanKits.length && i < humanSlots.length; i++) {
            HumanKit kit = humanKits[i];
            boolean selected = (kit == selectedHuman);
            inv.setItem(humanSlots[i], createKitItem(
                    Material.valueOf(kit.getIconMaterial()),
                    "§aKit Humano: §f" + kit.getName(),
                    kit.getDescription(),
                    selected
            ));
        }

        int[] infectedSlots = {19, 20, 21, 22, 23, 24};
        InfectedKit[] infectedKits = InfectedKit.values();
        for (int i = 0; i < infectedKits.length && i < infectedSlots.length; i++) {
            InfectedKit kit = infectedKits[i];
            boolean selected = (kit == selectedInfected);
            inv.setItem(infectedSlots[i], createKitItem(
                    Material.valueOf(kit.getIconMaterial()),
                    "§cKit Infectado: §f" + kit.getName(),
                    kit.getDescription(),
                    selected
            ));
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            if (item.getItemMeta().getDisplayName().contains("Selecionar Kit")) {
                event.setCancelled(true);
                openMenu(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if ("Selecionar Kits".equalsIgnoreCase(title)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

            String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
            GamePlayer gp = InfectionSoupAPI.getInstance().getPlayer(player.getUniqueId());
            if (gp == null) return;

            if (name.startsWith("Kit Humano:")) {
                String kitName = name.replace("Kit Humano:", "").trim();
                HumanKit hKit = HumanKit.fromName(kitName);
                gp.setHumanKit(hKit);
                player.sendMessage("§aVocê selecionou o kit Humano: §f" + hKit.getName());
                openMenu(player);
            } else if (name.startsWith("Kit Infectado:")) {
                String kitName = name.replace("Kit Infectado:", "").trim();
                InfectedKit iKit = InfectedKit.fromName(kitName);
                gp.setInfectedKit(iKit);
                player.sendMessage("§cVocê selecionou o kit Infectado: §f" + iKit.getName());
                openMenu(player);
            }
        }
    }

    private static ItemStack createKitItem(Material material, String name, String desc, boolean selected) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            String status = selected ? "§a§l[SELECIONADO]" : "§eClique para selecionar!";
            meta.setLore(Arrays.asList("§7" + desc, "§f", status));
            item.setItemMeta(meta);
        }
        return item;
    }
}
