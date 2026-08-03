package com.ascend.game.api.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameState {

    WAITING("Aguardando"),
    STARTING("Iniciando"),
    PREPARATION("Preparação"),
    INGAME("Em jogo"),
    ENDING("Encerrando");

    private final String displayName;
}
