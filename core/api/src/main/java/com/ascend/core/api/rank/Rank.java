package com.ascend.core.api.rank;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Rank {

    ADMIN       ("Admin",    "§4Admin",    "§4§lADMIN §4",    "rank.admin"),
    MOD_PLUS    ("Mod+",     "§5Mod+",     "§5§lMOD+ §5",     "rank.mod_plus"),
    MOD         ("Mod",      "§5Mod",      "§5§lMOD §5",      "rank.mod"),
    TRIAL       ("Trial",    "§5Trial",    "§5§lTRIAL §5",    "rank.trial"),
    PARTNER_PLUS("Plus",     "§bPlus",     "§b§lPLUS §b",     "rank.plus"),
    PARTNER     ("Partner",  "§bPartner",  "§b§lPARTNER §b",  "rank.partner"),
    BETA        ("Beta",     "§1Beta",     "§1§lBETA §1",     "rank.beta"),
    PARASITA    ("Parasita", "§2Parasita", "§2§lPARASITA §2", "rank.parasita"),
    ZUMBI       ("Zumbi",    "§aZumbi",    "§a§lZUMBI §a",    "rank.zumbi"),
    DEFAULT     ("Membro",   "§7Membro",   "§7",              "");

    private final String displayName;
    private final String coloredName;
    private final String defaultPrefix;
    private final String permission;

    public boolean isStaff() {
        return this == ADMIN || this == MOD_PLUS || this == MOD || this == TRIAL || this == PARTNER_PLUS || this == PARTNER;
    }
}
