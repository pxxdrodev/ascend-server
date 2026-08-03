package com.ascend.game.api.kit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InfectedKit {

    RUNNER("Runner", "Velocidade alta, ideal para caçar humanos.", "FEATHER"),
    BERSERKER("Berserker", "Dano elevado e força bruta contra humanos.", "IRON_AXE"),
    PHANTOM("Phantom", "Pode se tornar invisível temporariamente.", "GLASS_BOTTLE"),
    SPIDER("Spider", "Habilidade de escalar estruturas rapidamente.", "SPIDER_EYE"),
    VAMPIRE("Vampire", "Regenera vida ao infectar ou matar um humano.", "REDSTONE"),
    BOMBER("Bomber", "Explode ao morrer causando dano em área.", "TNT");

    private final String name;
    private final String description;
    private final String iconMaterial;

    public static InfectedKit fromName(String name) {
        if (name == null || name.isBlank()) return RUNNER;
        for (InfectedKit kit : values()) {
            if (kit.name().equalsIgnoreCase(name) || kit.getName().equalsIgnoreCase(name)) {
                return kit;
            }
        }
        return RUNNER;
    }
}
