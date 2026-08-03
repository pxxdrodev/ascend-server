package com.ascend.lobby.spigot.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SkinFetcher {

    private static final Map<String, String[]> SKIN_CACHE = new ConcurrentHashMap<>();

    public static String[] getSkin(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        String key = username.toLowerCase();
        if (SKIN_CACHE.containsKey(key)) {
            return SKIN_CACHE.get(key);
        }

        fetchSkinAsync(username, null);
        return null;
    }

    public static void fetchSkinAsync(String username, Runnable onComplete) {
        if (username == null || username.trim().isEmpty()) return;
        String key = username.toLowerCase();
        if (SKIN_CACHE.containsKey(key)) {
            if (onComplete != null) onComplete.run();
            return;
        }

        CompletableFuture.runAsync(() -> {
            boolean fetched = false;

            try {
                URL ashconUrl = new URL("https://api.ashcon.app/mojang/v2/user/" + username);
                HttpURLConnection conn = (HttpURLConnection) ashconUrl.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                if (conn.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();

                    if (json.has("textures") && json.getAsJsonObject("textures").has("raw")) {
                        JsonObject raw = json.getAsJsonObject("textures").getAsJsonObject("raw");
                        String value = raw.get("value").getAsString();
                        String signature = raw.has("signature") ? raw.get("signature").getAsString() : "";
                        SKIN_CACHE.put(key, new String[]{value, signature});
                        fetched = true;
                    }
                }
            } catch (Exception ignored) {}

            if (!fetched) {
                try {
                    URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
                    HttpURLConnection uuidConn = (HttpURLConnection) uuidUrl.openConnection();
                    uuidConn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    uuidConn.setConnectTimeout(4000);
                    uuidConn.setReadTimeout(4000);

                    if (uuidConn.getResponseCode() == 200) {
                        InputStreamReader uuidReader = new InputStreamReader(uuidConn.getInputStream(), StandardCharsets.UTF_8);
                        JsonObject uuidJson = new JsonParser().parse(uuidReader).getAsJsonObject();
                        if (uuidJson.has("id")) {
                            String mojangUuid = uuidJson.get("id").getAsString();

                            URL sessionUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + mojangUuid + "?unsigned=false");
                            HttpURLConnection sessionConn = (HttpURLConnection) sessionUrl.openConnection();
                            sessionConn.setRequestProperty("User-Agent", "Mozilla/5.0");
                            sessionConn.setConnectTimeout(4000);
                            sessionConn.setReadTimeout(4000);

                            if (sessionConn.getResponseCode() == 200) {
                                InputStreamReader sessionReader = new InputStreamReader(sessionConn.getInputStream(), StandardCharsets.UTF_8);
                                JsonObject sessionJson = new JsonParser().parse(sessionReader).getAsJsonObject();
                                if (sessionJson.has("properties")) {
                                    JsonArray properties = sessionJson.getAsJsonArray("properties");
                                    if (properties.size() > 0) {
                                        JsonObject prop = properties.get(0).getAsJsonObject();
                                        String val = prop.get("value").getAsString();
                                        String sig = prop.has("signature") ? prop.get("signature").getAsString() : "";
                                        SKIN_CACHE.put(key, new String[]{val, sig});
                                        fetched = true;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (fetched && onComplete != null) {
                try {
                    onComplete.run();
                } catch (Exception ignored) {}
            }
        });
    }
}
