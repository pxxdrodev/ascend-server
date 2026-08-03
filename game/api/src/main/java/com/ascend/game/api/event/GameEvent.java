package com.ascend.game.api.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameEvent {

    NONE("Nenhum", "Nenhum evento ativo."),
    NIGHT("Noite", "O mapa ficou escuro! Cuidado."),
    RAIN("Chuva", "Chuva forte reduz a visibilidade."),
    SUPPLY_DROP("Supply Drop", "Caixas de suprimentos surgiram no mapa!"),
    DOUBLE_COINS("Double Coins", "Ganho de moedas dobrado!"),
    SPEED("Super Velocidade", "Todos recebem velocidade aumentada!"),
    DOUBLE_DAMAGE("Dano Dobrado", "Todos causam o dobro de dano!"),
    LOW_GRAVITY("Gravidade Baixa", "Gravidade reduzida para todos!"),
    BLOOD_MOON("Lua de Sangue", "Infectados recebem buffs temporários!");

    private final String displayName;
    private final String description;
}
