package com.derko.advancedfoodsystem.gameplay;

import com.derko.seamlessapi.api.ComboRegistration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ComboEffectRegistry {
    private static final Map<String, Map<String, Double>> EFFECTS      = new HashMap<>();
    private static final Map<String, Set<String>>         REQUIREMENTS = new HashMap<>();
    private static final Set<String>                      CAPSTONE_IDS = new HashSet<>();
    private static final Set<String>                      FINAL_HEART_UNLOCK_IDS = new HashSet<>();

    static {
        // Speedster paths (requested): beetroot + salmon + potato OR cooked salmon + baked potato.
        pair("combo_speed_stride", map("walk_speed", 0.04D, "hunger_efficiency", 0.02D),
            "minecraft:beetroot", "minecraft:potato");
        pair("combo_speed_current", map("walk_speed", 0.04D, "hunger_efficiency", 0.02D),
            "minecraft:beetroot", "minecraft:salmon");
        capstone("combo_speedster_raw", map("walk_speed", 0.10D, "attack_speed", 0.05D, "frailty", 0.05D),
            "minecraft:beetroot", "minecraft:salmon", "minecraft:potato");
        capstone("combo_speedster_cooked", map("walk_speed", 0.10D, "attack_speed", 0.05D, "frailty", 0.05D),
            "minecraft:beetroot", "minecraft:cooked_salmon", "minecraft:baked_potato");

        // Mining path (small per-food gains, high only when fully stacked).
        pair("combo_miner_focus", map("mining_speed", 0.06D, "hunger_efficiency", 0.02D),
            "minecraft:dried_kelp", "minecraft:cookie");
        pair("combo_miner_study", map("mining_speed", 0.05D, "xp_gain", 0.03D),
            "minecraft:glow_berries", "minecraft:cookie");
        capstone("combo_quarry_engine", map("mining_speed", 0.20D, "xp_gain", 0.04D, "fatigue", 0.01D, "frailty", 0.04D),
            "minecraft:dried_kelp", "minecraft:glow_berries", "minecraft:cookie");

        // Tank path: max HP + resistance + knockback resistance with mobility/attack drawbacks.
        pair("combo_guarded_plate", map("damage_reduction", 0.04D, "knockback_resistance", 0.03D),
            "minecraft:cooked_porkchop", "minecraft:cooked_mutton");
        pair("combo_guarded_blessing", map("damage_reduction", 0.03D, "regeneration", 0.03D),
            "minecraft:golden_apple", "minecraft:cooked_mutton");
        capstone("combo_bulwark_core", map("damage_reduction", 0.08D, "knockback_resistance", 0.08D, "fatigue", 0.03D, "frailty", 0.03D),
            "minecraft:cooked_porkchop", "minecraft:cooked_mutton", "minecraft:golden_apple");

        // Fighting path: attack speed + regen, but higher food drain.
        pair("combo_duelist_line", map("attack_speed", 0.05D, "regeneration", 0.02D),
            "minecraft:cooked_chicken", "minecraft:cooked_rabbit");
        pair("combo_duelist_heart", map("attack_speed", 0.04D, "regeneration", 0.03D),
            "minecraft:apple", "minecraft:cooked_chicken");
        capstone("combo_skirmisher", map("attack_speed", 0.08D, "regeneration", 0.05D, "appetite_leak", 0.03D, "frailty", 0.04D),
            "minecraft:cooked_chicken", "minecraft:cooked_rabbit", "minecraft:apple");

        // XP path: efficient leveling with survivability drawback.
        pair("combo_scholar_path", map("xp_gain", 0.07D, "hunger_efficiency", 0.02D),
            "minecraft:cod", "minecraft:beetroot");
        pair("combo_scholar_path_cooked", map("xp_gain", 0.07D, "hunger_efficiency", 0.02D),
            "minecraft:cooked_cod", "minecraft:beetroot");
        capstone("combo_archivist", map("xp_gain", 0.14D, "hunger_efficiency", 0.03D, "frailty", 0.06D),
            "minecraft:cod", "minecraft:cooked_cod", "minecraft:glow_berries");

        // Balanced random/generalist paths.
        pair("combo_balanced_trail", map("walk_speed", 0.02D, "knockback_resistance", 0.02D),
            "minecraft:carrot", "minecraft:melon_slice");
        pair("combo_balanced_hearth", map("regeneration", 0.02D, "hunger_efficiency", 0.03D),
            "minecraft:bread", "minecraft:pumpkin_pie");
        pair("combo_balanced_tide", map("walk_speed", 0.02D, "xp_gain", 0.02D),
            "minecraft:tropical_fish", "minecraft:sweet_berries");
        capstone("combo_balanced_harmony", map("walk_speed", 0.02D, "attack_speed", 0.02D, "damage_reduction", 0.02D, "frailty", 0.03D),
            "minecraft:apple", "minecraft:bread", "minecraft:carrot");
    }

    private ComboEffectRegistry() {
    }

    public static Map<String, Double> effects(String comboId) {
        return EFFECTS.getOrDefault(comboId, Map.of());
    }

    public static boolean isCapstone(String comboId) {
        return CAPSTONE_IDS.contains(comboId);
    }

    public static boolean grantsFinalHeart(String comboId) {
        return FINAL_HEART_UNLOCK_IDS.contains(comboId);
    }

    /**
     * Returns the food item IDs required to trigger this combo.
     */
    public static Set<String> requiredFoods(String comboId) {
        return REQUIREMENTS.getOrDefault(comboId, Set.of());
    }

    /** Returns all combo IDs whose required foods are all present in activeFoodIds. */
    public static Set<String> activeCombos(Set<String> activeFoodIds) {
        Set<String> ids = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : REQUIREMENTS.entrySet()) {
            if (activeFoodIds.containsAll(entry.getValue())) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    /**
     * Merge external combo definitions registered through Seamless API.
     * Existing built-in combos are never overridden.
     */
    public static void mergeApiRegistrations(Map<String, ComboRegistration> comboEntries) {
        for (Map.Entry<String, ComboRegistration> entry : comboEntries.entrySet()) {
            String comboId = entry.getKey();
            ComboRegistration registration = entry.getValue();

            if (comboId == null || comboId.isBlank() || registration == null) {
                continue;
            }
            if (EFFECTS.containsKey(comboId) || REQUIREMENTS.containsKey(comboId)) {
                continue;
            }

            EFFECTS.put(comboId, Map.copyOf(registration.effects()));
            REQUIREMENTS.put(comboId, Set.copyOf(registration.requiredFoods()));

            if (registration.capstone()) {
                CAPSTONE_IDS.add(comboId);
            }
            if (registration.grantsFinalHeart()) {
                FINAL_HEART_UNLOCK_IDS.add(comboId);
            }
        }
    }

    private static void pair(String id, Map<String, Double> effects, String food1, String food2) {
        EFFECTS.put(id, effects);
        REQUIREMENTS.put(id, Set.of(food1, food2));
    }

    private static void capstone(String id, Map<String, Double> effects, String food1, String food2, String food3) {
        EFFECTS.put(id, effects);
        REQUIREMENTS.put(id, Set.of(food1, food2, food3));
        CAPSTONE_IDS.add(id);

        // Only selected capstones unlock the final heart.
        if ("combo_bulwark_core".equals(id)) {
            FINAL_HEART_UNLOCK_IDS.add(id);
        }
    }

    private static Map<String, Double> map(String k1, double v1) {
        Map<String, Double> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    private static Map<String, Double> map(String k1, double v1, String k2, double v2) {
        Map<String, Double> map = map(k1, v1);
        map.put(k2, v2);
        return map;
    }

    private static Map<String, Double> map(String k1, double v1, String k2, double v2, String k3, double v3) {
        Map<String, Double> map = map(k1, v1, k2, v2);
        map.put(k3, v3);
        return map;
    }

    private static Map<String, Double> map(String k1, double v1, String k2, double v2, String k3, double v3, String k4, double v4) {
        Map<String, Double> map = map(k1, v1, k2, v2, k3, v3);
        map.put(k4, v4);
        return map;
    }
}
