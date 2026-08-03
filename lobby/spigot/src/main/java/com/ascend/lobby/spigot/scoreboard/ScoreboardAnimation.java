package com.ascend.lobby.spigot.scoreboard;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardAnimation {

    private final List<String> frames = new ArrayList<>();
    private int index = 0;

    public ScoreboardAnimation() {
        String text = "ASCEND";

        frames.add("&b&l" + text);
        frames.add("&b&l" + text);

        for (int i = 0; i < text.length(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("&b&l");
            for (int j = 0; j < text.length(); j++) {
                if (i == j) {
                    sb.append("&e&l").append(text.charAt(j)).append("&b&l");
                } else {
                    sb.append(text.charAt(j));
                }
            }
            frames.add(sb.toString());
        }

        frames.add("&e&l" + text);
        frames.add("&e&l" + text);

        for (int i = 0; i < text.length(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("&e&l");
            for (int j = 0; j < text.length(); j++) {
                if (i == j) {
                    sb.append("&a&l").append(text.charAt(j)).append("&e&l");
                } else {
                    sb.append(text.charAt(j));
                }
            }
            frames.add(sb.toString());
        }

        frames.add("&a&l" + text);
        frames.add("&a&l" + text);

        for (int i = 0; i < text.length(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("&a&l");
            for (int j = 0; j < text.length(); j++) {
                if (i == j) {
                    sb.append("&b&l").append(text.charAt(j)).append("&a&l");
                } else {
                    sb.append(text.charAt(j));
                }
            }
            frames.add(sb.toString());
        }
    }

    public String nextFrame() {
        if (frames.isEmpty()) return ChatColor.AQUA + "" + ChatColor.BOLD + "ASCEND";
        String frame = frames.get(index % frames.size());
        index++;
        return ChatColor.translateAlternateColorCodes('&', frame);
    }
}
