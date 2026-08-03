package com.ascend.lobby.api;

public final class API {

    private static API instance;

    private API() {}

    public static API getInstance() {
        if (instance == null) {
            instance = new API();
        }
        return instance;
    }
}
