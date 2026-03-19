package com.derko.advancedfoodsystem.gameplay;

import java.util.HashMap;
import java.util.Map;

public final class BuffNames {
    private static final Map<String, String> NAMES = new HashMap<>();

    static {
        NAMES.put("mining_speed", "Mining Speed");
        NAMES.put("walk_speed", "Walk Speed");
        NAMES.put("jump_height", "Jump Height");
        NAMES.put("attack_speed", "Attack Speed");
        NAMES.put("damage_reduction", "Damage Reduction");
        NAMES.put("regeneration", "Regeneration");
        NAMES.put("saturation_boost", "Saturation Boost");
        NAMES.put("knockback_resistance", "Knockback Resistance");

        NAMES.put("warrior_boost", "Warrior Boost");
        NAMES.put("acrobat_boost", "Acrobat Boost");
        NAMES.put("guardian_boost", "Guardian Boost");
        NAMES.put("scholar_boost", "Scholar Boost");
    }

    private BuffNames() {
    }

    public static String pretty(String id) {
        return NAMES.getOrDefault(id, toWords(id));
    }

    public static String icon(String id) {
        return switch (id) {
            case "mining_speed" -> "\u26CF";
            case "walk_speed" -> "\uD83D\uDEB6";
            case "jump_height" -> "\u2B06";
            case "attack_speed", "warrior_boost" -> "\u2694";
            case "damage_reduction", "guardian_boost" -> "\uD83D\uDEE1";
            case "regeneration" -> "\u2764";
            case "saturation_boost", "scholar_boost" -> "\uD83C\uDF56";
            case "knockback_resistance" -> "\uD83E\uDEA8";
            case "acrobat_boost" -> "\uD83D\uDD75";
            default -> "*";
        };
    }

    private static String toWords(String id) {
        StringBuilder out = new StringBuilder();
        String[] parts = id.split("_");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (i > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                out.append(parts[i].substring(1));
            }
        }
        return out.toString();
    }
}
