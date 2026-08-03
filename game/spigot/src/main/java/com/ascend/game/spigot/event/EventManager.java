package com.ascend.game.spigot.event;

import com.ascend.game.api.event.GameEvent;
import com.ascend.game.api.player.GamePlayer;
import com.ascend.game.api.team.Team;
import com.ascend.game.api.InfectionSoupAPI;
import com.ascend.game.spigot.InfectionSoupPlugin;
import com.ascend.game.spigot.game.GameManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;

import org.bukkit.inventory.ItemStack;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

@Getter
public class EventManager {

    private final InfectionSoupPlugin plugin;
    private final Random random = new Random();

    private GameEvent activeEvent = GameEvent.NONE;
    private int eventDuration = 0;

    public EventManager(InfectionSoupPlugin plugin) {
        this.plugin = plugin;
    }

    public void tick() {
        if (activeEvent != GameEvent.NONE) {
            if (eventDuration > 0) {
                eventDuration--;
                applyContinuousEffects();
            } else {
                stopActiveEvent();
            }
        }
    }

    public void triggerRandomEvent() {
        GameEvent[] events = GameEvent.values();
        GameEvent newEvent = events[random.nextInt(events.length - 1) + 1];
        startEvent(newEvent);
    }

    public void startEvent(GameEvent event) {
        this.activeEvent = event;
        this.eventDuration = 60;

        GameManager game = plugin.getGameManager();
        if (game != null) {
            game.sendTitleAll("§e§lEVENTO: " + event.getDisplayName(), "§f" + event.getDescription());
            game.playSoundAll(Sound.WITHER_SPAWN, 0.8f, 1.0f);
        }

        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);

        switch (event) {
            case NIGHT:
                if (world != null) world.setTime(18000);
                break;

            case RAIN:
                if (world != null) {
                    world.setStorm(true);
                    world.setThundering(true);
                }
                break;

            case SUPPLY_DROP:
                spawnSupplyDrop();
                break;

            case SPEED:
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1));
                }
                break;

            case LOW_GRAVITY:
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1200, 2));
                }
                break;

            case BLOOD_MOON:
                if (world != null) world.setTime(18000);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    GamePlayer gp = InfectionSoupAPI.getInstance().getPlayer(p.getUniqueId());
                    if (gp != null && gp.getTeam() == Team.INFECTED) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1200, 0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1200, 0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 1));
                    }
                }
                break;

            default:
                break;
        }
    }

    private void applyContinuousEffects() {
        if (activeEvent == GameEvent.BLOOD_MOON) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                GamePlayer gp = InfectionSoupAPI.getInstance().getPlayer(p.getUniqueId());
                if (gp != null && gp.getTeam() == Team.INFECTED) {
                    p.getWorld().playEffect(p.getLocation(), org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
                }
            }
        }
    }

    public void stopActiveEvent() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world != null) {
            if (activeEvent == GameEvent.NIGHT || activeEvent == GameEvent.BLOOD_MOON) {
                world.setTime(6000);
            }
            if (activeEvent == GameEvent.RAIN) {
                world.setStorm(false);
                world.setThundering(false);
            }
        }

        GameManager game = plugin.getGameManager();
        if (game != null) {
            game.broadcast("§eO evento §f" + activeEvent.getDisplayName() + " §eterminou!");
        }

        this.activeEvent = GameEvent.NONE;
        this.eventDuration = 0;
    }

    private void spawnSupplyDrop() {
        Player randomPlayer = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (randomPlayer == null) return;

        Location loc = randomPlayer.getLocation().clone().add(random.nextInt(10) - 5, 0, random.nextInt(10) - 5);
        loc.setY(loc.getWorld().getHighestBlockYAt(loc));

        loc.getBlock().setType(Material.CHEST);
        if (loc.getBlock().getState() instanceof Chest) {
            Chest chest = (Chest) loc.getBlock().getState();
            chest.getInventory().setItem(0, new ItemStack(Material.MUSHROOM_SOUP, 4));
            chest.getInventory().setItem(4, new ItemStack(Material.GOLDEN_APPLE, 2));
            chest.getInventory().setItem(8, new ItemStack(Material.ENDER_PEARL, 2));
            chest.update();
        }

        GameManager game = plugin.getGameManager();
        if (game != null) {
            game.broadcast("§a§lSUPPLY DROP! §fUm baú de suprimentos caiu em X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ() + "!");
        }
    }

    public boolean isDoubleCoinsActive() {
        return activeEvent == GameEvent.DOUBLE_COINS;
    }

    public boolean isDoubleDamageActive() {
        return activeEvent == GameEvent.DOUBLE_DAMAGE;
    }
}
