package com.ascend.game.spigot.listener;

import com.ascend.core.api.utils.LocationUtils;
import com.ascend.game.api.InfectionSoupAPI;
import com.ascend.game.api.build.BuildManager;
import com.ascend.game.api.kit.InfectedKit;
import com.ascend.game.api.player.GamePlayer;
import com.ascend.game.api.state.GameState;
import com.ascend.game.api.team.Team;
import com.ascend.game.spigot.InfectionSoupPlugin;
import com.ascend.game.spigot.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameListener implements Listener {

    private final InfectionSoupPlugin plugin;
    private final Map<UUID, Long> PHANTOM_COOLDOWNS = new HashMap<>();

    public GameListener(InfectionSoupPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GameManager game = plugin.getGameManager();

        GamePlayer gp = new GamePlayer(player.getUniqueId(), player.getName());
        InfectionSoupAPI.getInstance().registerPlayer(gp);

        int online = Bukkit.getOnlinePlayers().size();
        event.setJoinMessage("§b" + player.getName() + " §eentrou na partida! §e(" + online + "/16)");

        plugin.getScoreboardManager().addPlayer(player);

        if (game != null && (game.getCurrentState() == GameState.WAITING || game.getCurrentState() == GameState.STARTING)) {
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

            giveWaitingItems(player);
        } else {
            gp.setTeam(Team.SPECTATOR);
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    private void giveWaitingItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aSelecionar Kit");
            meta.setLore(Collections.singletonList("§7Escolha suas classes para a partida!"));
            chest.setItemMeta(meta);
        }

        player.getInventory().setItem(0, chest);
        player.getInventory().setHeldItemSlot(0);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BuildManager.removeBuilder(player.getName());
        PHANTOM_COOLDOWNS.remove(player.getUniqueId());
        plugin.getScoreboardManager().removePlayer(player);
        InfectionSoupAPI.getInstance().unregisterPlayer(player.getUniqueId());

        int online = Bukkit.getOnlinePlayers().size() - 1;
        event.setQuitMessage("§b" + player.getName() + " §esaiu da partida! §e(" + online + "/16)");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        if (item.getItemMeta().getDisplayName().contains("Invisibilidade")) {
            Action action = event.getAction();
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                long now = System.currentTimeMillis();
                long last = PHANTOM_COOLDOWNS.getOrDefault(player.getUniqueId(), 0L);
                if (now - last < 30000) {
                    long remaining = (30000 - (now - last)) / 1000;
                    player.sendMessage("§cAguarde " + remaining + " segundos para usar a invisibilidade novamente.");
                    return;
                }

                PHANTOM_COOLDOWNS.put(player.getUniqueId(), now);
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0));
                player.sendMessage("§a§lPHANTOM! §eVocê está invisível por 5 segundos!");
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GamePlayer gp = InfectionSoupAPI.getInstance().getPlayer(player.getUniqueId());
        if (gp != null && gp.getTeam() == Team.INFECTED && gp.getInfectedKit() == InfectedKit.SPIDER) {
            if (player.isSneaking()) {
                Location loc = player.getLocation();
                if (loc.getBlock().getRelative(BlockFace.NORTH).getType().isSolid() ||
                    loc.getBlock().getRelative(BlockFace.SOUTH).getType().isSolid() ||
                    loc.getBlock().getRelative(BlockFace.EAST).getType().isSolid() ||
                    loc.getBlock().getRelative(BlockFace.WEST).getType().isSolid()) {
                    player.setVelocity(player.getVelocity().setY(0.2));
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!BuildManager.isBuilder(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!BuildManager.isBuilder(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        GameManager game = plugin.getGameManager();
        if (!BuildManager.isBuilder(player.getName()) && (game == null || game.getCurrentState() != GameState.INGAME)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        GameManager game = plugin.getGameManager();
        if (game != null) {
            game.handlePlayerDeath(victim, killer);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        GameManager game = plugin.getGameManager();
        if (game != null && (!game.isPvpEnabled() || game.getCurrentState() != GameState.INGAME)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        GamePlayer gpVictim = InfectionSoupAPI.getInstance().getPlayer(victim.getUniqueId());
        GamePlayer gpAttacker = InfectionSoupAPI.getInstance().getPlayer(attacker.getUniqueId());

        if (gpVictim != null && gpAttacker != null) {
            if (gpVictim.getTeam() == gpAttacker.getTeam()) {
                event.setCancelled(true);
                return;
            }
        }

        GameManager game = plugin.getGameManager();
        if (game != null && game.getEventManager() != null && game.getEventManager().isDoubleDamageActive()) {
            event.setDamage(event.getDamage() * 2.0);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        GameManager game = plugin.getGameManager();
        if (game != null && (game.getCurrentState() == GameState.WAITING || game.getCurrentState() == GameState.STARTING)) {
            event.setCancelled(true);
            event.setFoodLevel(20);
        }
    }
}
