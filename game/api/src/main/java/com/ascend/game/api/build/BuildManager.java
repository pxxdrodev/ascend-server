package com.ascend.game.api.build;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public final class BuildManager {

    @Getter
    private static final List<String> builders = new ArrayList<>();

    private BuildManager() {}

    public static boolean isBuilder(String username) {
        return builders.contains(username);
    }

    public static void addBuilder(String username) {
        if (!builders.contains(username)) {
            builders.add(username);
        }
    }

    public static void removeBuilder(String username) {
        builders.remove(username);
    }
}
