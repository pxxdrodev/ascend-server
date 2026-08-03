package com.ascend.game.api;

import lombok.Getter;

public final class GameAPI {

    @Getter
    private static GameAPI instance;

    private GameAPI() {}

    public static GameAPI getInstance() {
        if (instance == null) {
            instance = new GameAPI();
        }
        return instance;
    }
}
