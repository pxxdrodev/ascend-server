package com.ascend.lobby.spigot.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

        fetchSkinAsync(username);
        return null;
    }

    public static void fetchSkinAsync(String username) {
        if (username == null || username.trim().isEmpty()) return;
        String key = username.toLowerCase();
        if (SKIN_CACHE.containsKey(key)) return;

        CompletableFuture.runAsync(() -> {
            try {
                // 1. Busca o UUID real da conta na Mojang API
                URL uuidUrl = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
                HttpURLConnection uuidConn = (HttpURLConnection) uuidUrl.openConnection();
                uuidConn.setRequestProperty("User-Agent", "Mozilla/5.0");
                uuidConn.setConnectTimeout(5000);
                uuidConn.setReadTimeout(5000);

                if (uuidConn.getResponseCode() != 200) return;

                InputStreamReader uuidReader = new InputStreamReader(uuidConn.getInputStream(), StandardCharsets.UTF_8);
                JsonObject uuidJson = new JsonParser().parse(uuidReader).getAsJsonObject();
                if (!uuidJson.has("id")) return;
                String mojangUuid = uuidJson.get("id").getAsString();

                // 2. Busca os dados de textura (value e signature) no SessionServer da Mojang
                URL sessionUrl = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + mojangUuid + "?unsigned=false");
                HttpURLConnection sessionConn = (HttpURLConnection) sessionUrl.openConnection();
                sessionConn.setRequestProperty("User-Agent", "Mozilla/5.0");
                sessionConn.setConnectTimeout(5000);
                sessionConn.setReadTimeout(5000);

                if (sessionConn.getResponseCode() != 200) return;

                InputStreamReader sessionReader = new InputStreamReader(sessionConn.getInputStream(), StandardCharsets.UTF_8);
                JsonObject sessionJson = new JsonParser().parse(sessionReader).getAsJsonObject();
                if (!sessionJson.has("properties")) return;

                JsonArray properties = sessionJson.getAsJsonArray("properties");
                if (properties.size() > 0) {
                    JsonObject prop = properties.get(0).getAsJsonObject();
                    String val = prop.get("value").getAsString();
                    String sig = prop.has("signature") ? prop.get("signature").getAsString() : "";
                    SKIN_CACHE.put(key, new String[]{val, sig});
                }
            } catch (Exception ignored) {}
        });
    }
}
