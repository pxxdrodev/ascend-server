package com.ascend.game.api.player;

import com.ascend.game.api.kit.HumanKit;
import com.ascend.game.api.kit.InfectedKit;
import com.ascend.game.api.team.Team;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class GamePlayer {

    private final UUID uniqueId;
    private final String username;
    private Team team;
    private HumanKit humanKit;
    private InfectedKit infectedKit;

    private int kills;
    private int infections;
    private int coinsEarned;
    private int xpEarned;
    private boolean alive;
    private boolean respawning;

    public GamePlayer(UUID uniqueId, String username) {
        this.uniqueId = uniqueId;
        this.username = username;
        this.team = Team.HUMAN;
        this.humanKit = HumanKit.SOLDADO;
        this.infectedKit = InfectedKit.RUNNER;
        this.kills = 0;
        this.infections = 0;
        this.coinsEarned = 0;
        this.xpEarned = 0;
        this.alive = true;
        this.respawning = false;
    }

    public void addKill() {
        this.kills++;
    }

    public void addInfection() {
        this.infections++;
    }

    public void addCoins(int amount) {
        this.coinsEarned += amount;
    }

    public void addXp(int amount) {
        this.xpEarned += amount;
    }
}
