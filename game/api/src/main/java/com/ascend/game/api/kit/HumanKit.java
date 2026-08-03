package com.ascend.game.api.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum HumanKit {

    SOLDADO("Soldado", "Equipamento equilibrado para combate.", "IRON_SWORD"),
    MEDICO("Médico", "Possui mais sopas e regeneração rápida.", "MUSHROOM_SOUP"),
    ARQUEIRO("Arqueiro", "Equipado com arco e flechas para combate à distância.", "BOW"),
    TANQUE("Tanque", "Maior resistência e defesa reforçada.", "DIAMOND_CHESTPLATE"),
    EXPLORADOR("Explorador", "Recebe velocidade aumentada no início da partida.", "FEATHER");

    private final String name;
    private final String description;
    private final String iconMaterial;

    public static HumanKit fromName(String name) {
        if (name == null || name.isBlank()) return SOLDADO;
        for (HumanKit kit : values()) {
            if (kit.name().equalsIgnoreCase(name) || kit.getName().equalsIgnoreCase(name)) {
                return kit;
            }
        }
        return SOLDADO;
    }
}
