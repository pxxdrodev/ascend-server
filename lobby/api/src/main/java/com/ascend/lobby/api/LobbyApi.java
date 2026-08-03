package com.ascend.lobby.api;

public final class LobbyApi {

    private static LobbyApi instance;

    private LobbyApi() {}

    public static LobbyApi getInstance() {
        if (instance == null) {
            instance = new LobbyApi();
        }
        return instance;
    }
}
