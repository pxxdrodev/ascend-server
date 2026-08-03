package com.ascend.game.api.team;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Team {

    HUMAN("Humano", "§a", "§a§lHUMANO"),
    INFECTED("Infectado", "§c", "§c§lINFECTADO"),
    SPECTATOR("Espectador", "§7", "§7§lESPECTADOR");

    private final String name;
    private final String color;
    private final String formattedName;
}
