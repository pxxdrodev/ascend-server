package com.ascend.game.api;

import com.ascend.game.api.player.GamePlayer;
import com.ascend.game.api.state.GameState;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InfectionSoupAPI {

    @Getter
    private static InfectionSoupAPI instance;

    @Getter
    @Setter
    private GameState currentState = GameState.WAITING;

    private final Map<UUID, GamePlayer> players = new ConcurrentHashMap<>();

    private InfectionSoupAPI() {}

    public static InfectionSoupAPI getInstance() {
        if (instance == null) {
            instance = new InfectionSoupAPI();
        }
        return instance;
    }

    public GamePlayer getPlayer(UUID uuid) {
        if (uuid == null) return null;
        return players.get(uuid);
    }

    public void registerPlayer(GamePlayer player) {
        if (player != null && player.getUniqueId() != null) {
            players.put(player.getUniqueId(), player);
        }
    }

    public void unregisterPlayer(UUID uuid) {
        if (uuid != null) {
            players.remove(uuid);
        }
    }

    public Map<UUID, GamePlayer> getPlayers() {
        return players;
    }
}
