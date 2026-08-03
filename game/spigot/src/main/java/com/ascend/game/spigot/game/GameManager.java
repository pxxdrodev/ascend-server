package com.ascend.game.spigot.game;

import com.ascend.core.api.level.LevelManager;
import com.ascend.core.api.nms.NMSHelper;
import com.ascend.core.api.utils.LocationUtils;
import com.ascend.game.api.InfectionSoupAPI;
import com.ascend.game.api.event.GameEvent;
import com.ascend.game.api.kit.HumanKit;
import com.ascend.game.api.kit.InfectedKit;
import com.ascend.game.api.player.GamePlayer;
import com.ascend.game.api.state.GameState;
import com.ascend.game.api.stats.InfectionSoupStats;
import com.ascend.game.api.team.Team;
import com.ascend.game.spigot.InfectionSoupPlugin;
import com.ascend.game.spigot.event.EventManager;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Getter
public class GameManager {

    private final InfectionSoupPlugin plugin;
    private final EventManager eventManager;
    private final Random random = new Random();

    private GameState currentState = GameState.WAITING;

    private int timer = 0;
    private int eventTimer = 120; // Evento a cada 2 minutos
    private boolean pvpEnabled = false;

    public GameManager(InfectionSoupPlugin plugin) {
        this.plugin = plugin;
        this.eventManager = new EventManager(plugin);
        startMainTicker();
    }

    public void startMainTicker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            InfectionSoupAPI.getInstance().setCurrentState(currentState);

            switch (currentState) {
                case WAITING:
                    handleWaiting();
                    break;
                case STARTING:
                    handleStarting();
                    break;
                case PREPARATION:
                    handlePreparation();
                    break;
                case INGAME:
                    handleIngame();
                    break;
                case ENDING:
                    handleEnding();
                    break;
            }
        }, 20L, 20L);
    }

    private void handleWaiting() {
        int online = Bukkit.getOnlinePlayers().size();
        if (online >= 2) {
            currentState = GameState.STARTING;
            timer = 30;
        }
    }

    private void handleStarting() {
        int online = Bukkit.getOnlinePlayers().size();
        if (online < 2) {
            currentState = GameState.WAITING;
            timer = 0;
            broadcast("§cA partida foi cancelada por falta de jogadores!");
            return;
        }

        if (timer == 30 || timer == 15 || timer == 10 || (timer <= 5 && timer > 0)) {
            sendTitleAll("§b§lINICIANDO A PARTIDA EM", "§e" + timer + " segundo" + (timer > 1 ? "s" : ""));
            playSoundAll(Sound.NOTE_PLING, 1.0f, 1.0f);
        }

        if (timer <= 0) {
            startPreparation();
        } else {
            timer--;
        }
    }

    private void startPreparation() {
        currentState = GameState.PREPARATION;
        timer = 10;
        pvpEnabled = false;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);

        int infectedCount = players.size() >= 10 ? 2 : 1;

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            GamePlayer gp = InfectionSoupAPI.getInstance().getPlayer(p.getUniqueId());
            if (gp == null) {
                gp = new GamePlayer(p.getUniqueId(), p.getName());
                InfectionSoupAPI.getInstance().registerPlayer(gp);
            }

            if (i < infectedCount) {
                gp.setTeam(Team.INFECTED);
            } else {
                gp.setTeam(Team.HUMAN);
            }
            gp.setAlive(true);
        }

        sendTitleAll("§a§lPREPARE-SE!", "§eA infecção vai começar...");
        teleportToArenaAll();
    }

    private void handlePreparation() {
        if (timer <= 0) {
            startIngame();
        } else {
            sendTitleAll("§a§lPREPARE-SE!", "§eEm " + timer + " segundo" + (timer > 1 ? "s" : ""));
            timer--;
        }
    }

    private void startIngame() {
        currentState = GameState.INGAME;
        timer = 600; // 10 minutos
        eventTimer = 120;
        pvpEnabled = true;

        for (Player p : Bukkit.getOnlinePlayers()) {
            GamePlayer gp = InfectionSoupAPI.getInstance().getPlayer(p.getUniqueId());
            if (gp == null) continue;

            if (gp.getTeam() == Team.HUMAN) {
                sendTitle(p, "§a§lSOBREVIVA!", "§eVocê é um Humano!");
                equipHumanKit(p, gp.getHumanKit());
            } else {
                sendTitle(p, "§c§lINFECTE TODOS!", "§cVocê é o Primeiro Infectado!");
                equipInfectedKit(p, gp.getInfectedKit());
            }
        }
    }

    private void handleIngame() {
        if (timer <= 0) {
            endGame(Team.HUMAN);
            return;
        }

        int humans = countTeam(Team.HUMAN);
        if (humans == 0) {
            endGame(Team.INFECTED);
            return;
        }

        if (eventTimer <= 0) {
            eventManager.triggerRandomEvent();
            eventTimer = 120;
        } else {
            eventTimer--;
        }

        eventManager.tick();
        timer--;
    }

    public void handlePlayerDeath(Player victim, Player killer) {
        GamePlayer gpVictim = InfectionSoupAPI.getInstance().getPlayer(victim.getUniqueId());
        if (gpVictim == null) return;

        if (killer != null) {
            GamePlayer gpKiller = InfectionSoupAPI.getInstance().getPlayer(killer.getUniqueId());
            if (gpKiller != null) {
                int coinBonus = eventManager.isDoubleCoinsActive() ? 40 : 20;
                gpKiller.addKill();
                gpKiller.addCoins(coinBonus);
                gpKiller.addXp(50);

                if (gpKiller.getTeam() == Team.INFECTED) {
                    gpKiller.addInfection();
                    if (gpKiller.getInfectedKit() == InfectedKit.VAMPIRE) {
                        killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                        killer.setHealth(Math.min(killer.getHealth() + 4.0, killer.getMaxHealth()));
                        killer.sendMessage("§c§lVAMPIRE! §eVocê recuperou vida ao infectar um humano!");
                    }
                }
            }
        }

        if (gpVictim.getTeam() == Team.INFECTED && gpVictim.getInfectedKit() == InfectedKit.BOMBER) {
            Location loc = victim.getLocation();
            loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 2.0f, false, false);
            broadcast("§c§lBOMBER! §c" + victim.getName() + " explodiu ao morrer!");
        }

        if (gpVictim.getTeam() == Team.HUMAN) {
            gpVictim.setTeam(Team.INFECTED);
            broadcast("§c" + victim.getName() + " foi infectado!");
        }

        gpVictim.setRespawning(true);
        sendTitle(victim, "§c§lVOCÊ MORREU!", "§eRenasce em 3 segundos...");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            victim.spigot().respawn();
            gpVictim.setRespawning(false);
            gpVictim.setAlive(true);
            teleportToArena(victim);
            equipInfectedKit(victim, gpVictim.getInfectedKit());
        }, 60L);
    }

    private void endGame(Team winnerTeam) {
        currentState = GameState.ENDING;
        timer = 10;
        pvpEnabled = false;

        if (eventManager != null) {
            eventManager.stopActiveEvent();
        }

        if (winnerTeam == Team.HUMAN) {
            sendTitleAll("§a§lVITÓRIA DOS HUMANOS!", "§eOs humanos sobreviveram à infecção!");
        } else {
            sendTitleAll("§c§lVITÓRIA DOS INFECTADOS!", "§cTodos os humanos foram infectados!");
        }

        saveAllPlayerStats(winnerTeam);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                connectToLobby(p);
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.shutdown(), 60L);
        }, 200L);
    }

    private void saveAllPlayerStats(Team winnerTeam) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            GamePlayer gp = InfectionSoupAPI.getInstance().getPlayer(player.getUniqueId());
            if (gp == null) continue;

            InfectionSoupStats stats = InfectionSoupStats.load(player.getUniqueId(), player.getName(), plugin.getRedisConnection(), plugin.getMongoConnection());

            boolean won = (gp.getTeam() == winnerTeam);
            if (won) {
                stats.setWins(stats.getWins() + 1);
                gp.addCoins(100);
                gp.addXp(200);
            } else {
                stats.setLosses(stats.getLosses() + 1);
                gp.addCoins(30);
                gp.addXp(50);
            }

            if (gp.getTeam() == Team.HUMAN && won) {
                stats.setHumansSurvived(stats.getHumansSurvived() + 1);
            }
            stats.setHumansInfected(stats.getHumansInfected() + gp.getInfections());
            stats.setInfectedKilled(stats.getInfectedKilled() + gp.getKills());
            stats.setCoins(stats.getCoins() + gp.getCoinsEarned());

            stats.save(plugin.getRedisConnection(), plugin.getMongoConnection());

            boolean leveledUp = LevelManager.addXp(player.getUniqueId(), player.getName(), gp.getXpEarned(), plugin.getRedisConnection(), plugin.getMongoConnection());
            int newLevel = LevelManager.getLevel(player.getName(), plugin.getRedisConnection(), plugin.getMongoConnection());

            player.sendMessage("§a§l[FIM DA PARTIDA]");
            player.sendMessage("§fMoedas ganhas: §a+" + gp.getCoinsEarned());
            player.sendMessage("§fXP ganho: §b+" + gp.getXpEarned());

            if (leveledUp) {
                sendTitle(player, "§a§lLEVEL UP!", "§eVocê subiu para o Nível " + newLevel + "!");
                playSoundAll(Sound.LEVEL_UP, 1.0f, 1.0f);
            }
        }
    }

    private void handleEnding() {
        if (timer > 0) timer--;
    }

    public void stopGame() {
        currentState = GameState.WAITING;
        timer = 0;
        pvpEnabled = false;
        if (eventManager != null) {
            eventManager.stopActiveEvent();
        }
    }

    public void equipHumanKit(Player player, HumanKit kit) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        if (kit == HumanKit.TANQUE) {
            player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 99999, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 99999, 0));
        } else {
            player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        }

        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        int soups = (kit == HumanKit.MEDICO) ? 16 : 8;
        for (int i = 1; i <= soups && i < 9; i++) {
            player.getInventory().setItem(i, new ItemStack(Material.MUSHROOM_SOUP));
        }

        if (kit == HumanKit.MEDICO) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 99999, 0));
        } else if (kit == HumanKit.ARQUEIRO) {
            player.getInventory().setItem(8, new ItemStack(Material.BOW));
            player.getInventory().addItem(new ItemStack(Material.ARROW, 32));
        } else if (kit == HumanKit.EXPLORADOR) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1));
        }
    }

    public void equipInfectedKit(Player player, InfectedKit kit) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) helmet.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.RED);
            helmet.setItemMeta(meta);
        }

        player.getInventory().setHelmet(helmet);
        player.getInventory().setItem(0, new ItemStack(Material.STONE_SWORD));
        player.getInventory().setItem(1, new ItemStack(Material.MUSHROOM_SOUP));
        player.getInventory().setItem(2, new ItemStack(Material.MUSHROOM_SOUP));

        if (kit == InfectedKit.RUNNER) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 1));
        } else if (kit == InfectedKit.BERSERKER) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 99999, 0));
        } else if (kit == InfectedKit.SPIDER) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 99999, 1));
        } else if (kit == InfectedKit.PHANTOM) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0));
            ItemStack bottle = new ItemStack(Material.GLASS_BOTTLE);
            ItemMeta bMeta = bottle.getItemMeta();
            if (bMeta != null) {
                bMeta.setDisplayName("§aInvisibilidade (Clique)");
                bottle.setItemMeta(bMeta);
            }
            player.getInventory().setItem(8, bottle);
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0));
        }
    }

    public void teleportToArenaAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            teleportToArena(p);
        }
    }

    public void teleportToArena(Player player) {
        String rawLoc = plugin.getConfig().getString("config.locations.arena");
        Location loc = LocationUtils.deserializeLocation(rawLoc);
        if (loc != null && loc.getWorld() != null) {
            player.teleport(loc);
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }
    }

    public void connectToLobby(Player player) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF("lobby");
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } catch (Exception ignored) {}
    }

    public int countTeam(Team team) {
        int count = 0;
        for (GamePlayer gp : InfectionSoupAPI.getInstance().getPlayers().values()) {
            if (gp.getTeam() == team && gp.isAlive()) count++;
        }
        return count;
    }

    public String getFormattedTime() {
        int minutes = timer / 60;
        int seconds = timer % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public GameEvent getCurrentEvent() {
        return eventManager != null ? eventManager.getActiveEvent() : GameEvent.NONE;
    }

    public void broadcast(String msg) {
        Bukkit.broadcastMessage(msg);
    }

    public void sendTitleAll(String title, String subtitle) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendTitle(p, title, subtitle);
        }
    }

    public void sendTitle(Player player, String title, String subtitle) {
        NMSHelper.sendTitle(player, title, subtitle, 10, 40, 10);
    }

    public void playSoundAll(Sound sound, float vol, float pitch) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), sound, vol, pitch);
        }
    }
}
