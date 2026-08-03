package com.ascend.lobby.api.listener;

import com.ascend.core.api.account.Account;
import com.ascend.core.api.account.AccountCache;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.tag.Tag;
import com.ascend.core.api.utils.LocationUtils;
import com.ascend.lobby.api.bossbar.BossBarManager;
import com.ascend.lobby.api.menu.LobbyMenuManager;
import com.ascend.lobby.api.title.TitleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LobbyPlayerListener implements Listener {

    private final Plugin plugin;
    private final BossBarManager bossBarManager;
    private static final Set<UUID> HIDDEN_PLAYERS = new HashSet<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    public LobbyPlayerListener(Plugin plugin) {
        this.plugin = plugin;
        this.bossBarManager = new BossBarManager(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        Player player = event.getPlayer();

        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);

        String rawSpawn = plugin.getConfig().getString("config.locations.spawn");
        if (rawSpawn != null) {
            Location spawnLoc = LocationUtils.deserializeLocation(rawSpawn);
            if (spawnLoc != null && spawnLoc.getWorld() != null) {
                player.teleport(spawnLoc);
            }
        }

        for (int i = 0; i < 100; i++) {
            player.sendMessage("");
        }

        giveLobbyItems(player);

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (HIDDEN_PLAYERS.contains(other.getUniqueId())) {
                other.hidePlayer(player);
            }
            if (HIDDEN_PLAYERS.contains(player.getUniqueId())) {
                player.hidePlayer(other);
            }
        }

        TitleManager.sendRandomWelcomeTitle(player);
        bossBarManager.addPlayer(player);

        Tag tag = getPlayerActiveTag(player);
        if (tag != null && tag != Tag.MEMBRO) {
            Bukkit.broadcastMessage(color(tag.getPrefixColored() + player.getName() + " &6entrou no lobby!"));
        }
    }

    private void giveLobbyItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        player.getInventory().setItem(0, createItem(Material.COMPASS, (short) 0, "§aSelecionar jogo", "§7Abra o menu de jogos!"));
        player.getInventory().setItem(1, createSkull(player.getName(), "§aPerfil", "§7Veja algumas de suas informações."));
        player.getInventory().setItem(4, createItem(Material.CHEST, (short) 0, "§aColecionáveis", "§7Selecione seus colecionáveis!"));

        if (HIDDEN_PLAYERS.contains(player.getUniqueId())) {
            player.getInventory().setItem(7, createItem(Material.INK_SACK, (short) 8, "§cJogadores: §fOcultos", "§7Clique para mostrar os jogadores."));
        } else {
            player.getInventory().setItem(7, createItem(Material.INK_SACK, (short) 10, "§aJogadores: §fVisíveis", "§7Clique para ocultar os jogadores."));
        }

        player.getInventory().setItem(8, createItem(Material.WATCH, (short) 0, "§aLobbies", "§7Conecte-se a outro lobby!"));

        player.getInventory().setHeldItemSlot(0);
        player.updateInventory();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            String name = item.getItemMeta().getDisplayName();

            if (name.contains("Selecionar jogo")) {
                event.setCancelled(true);
                LobbyMenuManager.openGameSelector(player, getRedisConnection());
            } else if (name.contains("Perfil")) {
                event.setCancelled(true);
                LobbyMenuManager.openProfile(player);
            } else if (name.contains("Lobbies")) {
                event.setCancelled(true);
                LobbyMenuManager.openLobbySelector(player, getRedisConnection());
            } else if (name.contains("Jogadores:")) {
                event.setCancelled(true);
                long now = System.currentTimeMillis();
                long last = COOLDOWNS.getOrDefault(player.getUniqueId(), 0L);
                if (now - last < 500) {
                    return;
                }
                COOLDOWNS.put(player.getUniqueId(), now);
                togglePlayerVisibility(player);
            } else if (name.contains("Colecionáveis")) {
                event.setCancelled(true);
            }
        }
    }

    private void togglePlayerVisibility(Player player) {
        UUID uuid = player.getUniqueId();
        if (HIDDEN_PLAYERS.contains(uuid)) {
            HIDDEN_PLAYERS.remove(uuid);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) {
                    player.showPlayer(other);
                }
            }
            player.getInventory().setItem(7, createItem(Material.INK_SACK, (short) 10, "§aJogadores: §fVisíveis", "§7Clique para ocultar os jogadores."));
            player.sendMessage(color("&aVocê tornou todos os jogadores do lobby visíveis."));
        } else {
            HIDDEN_PLAYERS.add(uuid);
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) {
                    player.hidePlayer(other);
                }
            }
            player.getInventory().setItem(7, createItem(Material.INK_SACK, (short) 8, "§cJogadores: §fOcultos", "§7Clique para mostrar os jogadores."));
            player.sendMessage(color("&cVocê ocultou todos os jogadores do lobby."));
        }
        player.updateInventory();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        if ("Selecionar jogo".equalsIgnoreCase(title)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                if (clicked.getItemMeta().getDisplayName().contains("Infection Soup")) {
                    LobbyMenuManager.connectToBestInfectionRoom(plugin, player, getRedisConnection());
                }
            }
            return;
        }

        if ("Perfil".equalsIgnoreCase(title)) {
            event.setCancelled(true);
            return;
        }

        if ("Selecionar lobby".equalsIgnoreCase(title)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                if (clicked.getItemMeta().getDisplayName().contains("Lobby #1")) {
                    player.sendMessage(color("&cVocê já está conectado aqui!"));
                }
            }
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        bossBarManager.removePlayer(event.getPlayer());
        HIDDEN_PLAYERS.remove(event.getPlayer().getUniqueId());
        COOLDOWNS.remove(event.getPlayer().getUniqueId());
    }

    private RedisConnect getRedisConnection() {
        try {
            var method = plugin.getClass().getMethod("getRedisConnection");
            Object res = method.invoke(plugin);
            if (res instanceof RedisConnect) return (RedisConnect) res;
        } catch (Exception ignored) {}
        return null;
    }

    private Tag getPlayerActiveTag(Player player) {
        Account account = AccountCache.getAccount(player.getUniqueId());
        if (account != null && account.getTag() != null) {
            return account.getTag();
        }
        try {
            Class<?> clazz = Class.forName("com.ascend.core.spigot.tag.TagManager");
            var method = clazz.getMethod("getPlayerTag", Player.class);
            Object res = method.invoke(null, player);
            if (res instanceof Tag) return (Tag) res;
        } catch (Exception ignored) {}
        return Tag.MEMBRO;
    }

    private ItemStack createItem(Material material, short damage, String name, String loreLine) {
        ItemStack item = new ItemStack(material, 1, damage);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLine != null && !loreLine.isBlank()) {
                meta.setLore(Collections.singletonList(loreLine));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSkull(String owner, String name, String loreLine) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwner(owner);
            meta.setDisplayName(name);
            if (loreLine != null && !loreLine.isBlank()) {
                meta.setLore(Collections.singletonList(loreLine));
            }
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
