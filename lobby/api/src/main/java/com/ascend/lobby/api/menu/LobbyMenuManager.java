package com.ascend.lobby.api.menu;

import com.ascend.core.api.account.Account;
import com.ascend.core.api.account.AccountCache;
import com.ascend.core.api.backend.redis.RedisConnect;
import com.ascend.core.api.rank.Rank;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LobbyMenuManager {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final List<String> INFECTION_ROOMS = List.of("infectionsoup-1", "infectionsoup-2", "infectionsoup-3", "infectionsoup");

    private LobbyMenuManager() {}

    public static void openGameSelector(Player player, RedisConnect redis) {
        Inventory inv = Bukkit.createInventory(null, 45, "Selecionar jogo");

        int totalOnlineInfection = getTotalInfectionOnline(redis);
        ItemStack zombieHead = new ItemStack(Material.SKULL_ITEM, 1, (short) 2);
        ItemMeta zMeta = zombieHead.getItemMeta();
        if (zMeta != null) {
            zMeta.setDisplayName("§aInfection Soup");
            zMeta.setLore(Arrays.asList("§e" + totalOnlineInfection + " jogando agora!"));
            zombieHead.setItemMeta(zMeta);
        }
        inv.setItem(22, zombieHead);

        player.openInventory(inv);
    }

    public static void openProfile(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "Perfil");

        Account account = AccountCache.getAccount(player.getUniqueId());
        Rank rank = account != null ? account.getRank() : Rank.DEFAULT;
        String nowStr = DATE_FORMAT.format(System.currentTimeMillis());

        ItemStack head = createSkull(
                player.getName(),
                "§aInformações da conta",
                "§fRank: " + rank.getColoredName(),
                "§f",
                "§fPrimeiro login: §7" + nowStr,
                "§fÚltimo login: §7" + nowStr
        );
        inv.setItem(22, head);

        player.openInventory(inv);
    }

    public static void openLobbySelector(Player player, RedisConnect redis) {
        Inventory inv = Bukkit.createInventory(null, 45, "Selecionar lobby");

        int onlineLobby = getOnlineCount(redis, "lobby");
        if (onlineLobby <= 0) onlineLobby = Bukkit.getOnlinePlayers().size();

        ItemStack currentLobby = createItem(
                Material.STAINED_GLASS_PANE,
                (short) 5,
                "§aASCEND Lobby #1",
                "§fJogadores: §7" + onlineLobby + "/30",
                "§f",
                "§aVocê já está conectado aqui!"
        );
        inv.setItem(11, currentLobby);

        player.openInventory(inv);
    }

    public static void connectToBestInfectionRoom(Plugin plugin, Player player, RedisConnect redis) {
        String bestRoom = findBestInfectionSoupRoom(redis);
        connectToServer(plugin, player, bestRoom);
    }

    public static String findBestInfectionSoupRoom(RedisConnect redis) {
        String bestRoom = "infectionsoup-1";
        int maxPlayers = -1;

        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                for (String room : INFECTION_ROOMS) {
                    String val = jedis.hget("servers:online", room.toLowerCase(Locale.ROOT));
                    if (val != null) {
                        try {
                            int count = Integer.parseInt(val);
                            if (count < 32 && count > maxPlayers) {
                                maxPlayers = count;
                                bestRoom = room;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }
        return bestRoom;
    }

    public static void connectToServer(Plugin plugin, Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            player.sendMessage("§cNão foi possível conectar ao servidor '" + serverName + "'.");
        }
    }

    private static int getTotalInfectionOnline(RedisConnect redis) {
        int total = 0;
        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                for (String room : INFECTION_ROOMS) {
                    String val = jedis.hget("servers:online", room);
                    if (val != null) {
                        try { total += Integer.parseInt(val); } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }
        return total;
    }

    private static int getOnlineCount(RedisConnect redis, String serverName) {
        if (redis != null && redis.isConnected()) {
            try (Jedis jedis = redis.getResource()) {
                String val = jedis.hget("servers:online", serverName);
                if (val != null) {
                    return Integer.parseInt(val);
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private static ItemStack createItem(Material material, short damage, String name, String... loreLines) {
        ItemStack item = new ItemStack(material, 1, damage);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLines != null && loreLines.length > 0) {
                meta.setLore(Arrays.asList(loreLines));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createSkull(String owner, String name, String... loreLines) {
        ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwner(owner);
            meta.setDisplayName(name);
            if (loreLines != null && loreLines.length > 0) {
                meta.setLore(Arrays.asList(loreLines));
            }
            skull.setItemMeta(meta);
        }
        return skull;
    }
}
