package com.ascend.core.api.tag;

import com.ascend.core.api.rank.Rank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum Tag {

    ADMIN       (Rank.ADMIN,        "§4§lADMIN §4",   "§4Admin"),
    MOD_PLUS    (Rank.MOD_PLUS,     "§5§lMOD+ §5",    "§5Mod+"),
    MOD         (Rank.MOD,          "§5§lMOD §5",     "§5Mod"),
    TRIAL       (Rank.TRIAL,        "§5§lTRIAL §5",   "§5Trial"),
    PARTNER_PLUS(Rank.PARTNER_PLUS, "§b§lPLUS §b",    "§bPlus"),
    PARTNER     (Rank.PARTNER,      "§b§lPARTNER §b", "§bPartner"),
    BETA        (Rank.BETA,         "§1§lBETA §1",    "§1Beta"),
    PARASITA    (Rank.PARASITA,     "§2§lPARASITA §2","§2Parasita"),
    ZUMBI       (Rank.ZUMBI,        "§a§lZUMBI §a",   "§aZumbi"),
    MEMBRO      (Rank.DEFAULT,      "§7",             "§7Membro");

    private final Rank rank;
    private final String prefix;
    private final String nameFormatted;

    public String getPrefixColored() {
        return prefix;
    }

    public String getNameFormattedColored() {
        return nameFormatted;
    }

    public static Tag fromName(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String clean = input.toUpperCase(Locale.ROOT).replace("+", "_PLUS");
        if (clean.equals("PLUS") || clean.equals("PARTNERPLUS")) return PARTNER_PLUS;
        if (clean.equals("MODPLUS")) return MOD_PLUS;

        for (Tag tag : values()) {
            if (tag.name().equalsIgnoreCase(clean) ||
                tag.name().replace("_", "").equalsIgnoreCase(clean)) {
                return tag;
            }
        }
        return null;
    }

    public static Tag fromRank(Rank rank) {
        if (rank == null) return MEMBRO;
        for (Tag tag : values()) {
            if (tag.getRank() == rank) return tag;
        }
        return MEMBRO;
    }
}
