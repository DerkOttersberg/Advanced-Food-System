package com.derko.advancedfoodsystem.gameplay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ComboEffectRegistry {
    private static final Map<String, Map<String, Double>> EFFECTS      = new HashMap<>();
    private static final Map<String, Set<String>>         REQUIREMENTS = new HashMap<>();
    private static final Set<String>                      CAPSTONE_IDS = new HashSet<>();

    static {
    pair("combo_predator",      map("attack_speed", 0.05D, "attack_damage", 0.05D),
        "minecraft:cooked_chicken", "minecraft:cooked_rabbit");
    pair("combo_bastion",       map("damage_reduction", 0.05D, "knockback_resistance", 0.08D),
        "minecraft:cooked_porkchop", "minecraft:cooked_mutton");
    pair("combo_warden",        map("damage_reduction", 0.04D, "regeneration", 0.04D),
        "minecraft:golden_apple", "minecraft:golden_carrot");
    pair("combo_renewal",       map("regeneration", 0.08D),
        "minecraft:apple", "minecraft:cooked_salmon");
    pair("combo_windstep",      map("walk_speed", 0.10D),
        "minecraft:carrot", "minecraft:chorus_fruit");
    pair("combo_scholar",       map("xp_gain", 0.12D),
        "minecraft:dried_kelp", "minecraft:glow_berries");
    pair("combo_hunter",        map("attack_damage", 0.04D, "xp_gain", 0.08D),
        "minecraft:cooked_beef", "minecraft:beetroot");
    pair("combo_steward",       map("regeneration", 0.04D, "hunger_efficiency", 0.08D),
        "minecraft:bread", "minecraft:pumpkin_pie");
    pair("combo_duelist",       map("attack_speed", 0.04D, "walk_speed", 0.06D),
        "minecraft:sweet_berries", "minecraft:tropical_fish");
    capstone("combo_bruiser_prime",    map("attack_damage", 0.05D, "damage_reduction", 0.05D, "regeneration", 0.05D),
        "minecraft:beef", "minecraft:porkchop", "minecraft:mutton");
    capstone("combo_blood_dancer",     map("attack_speed", 0.05D, "regeneration", 0.05D, "walk_speed", 0.06D),
        "minecraft:chicken", "minecraft:rabbit", "minecraft:salmon");
    capstone("combo_cursed_chain",     map("attack_damage", 0.05D, "xp_gain", 0.08D, "frailty", 0.02D),
        "minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:poisonous_potato");
    capstone("combo_iron_sustainer",   map("damage_reduction", 0.05D, "regeneration", 0.05D, "hunger_efficiency", 0.10D),
        "minecraft:baked_potato", "minecraft:potato", "minecraft:cake");
    capstone("combo_expedition_guard", map("damage_reduction", 0.04D, "walk_speed", 0.05D, "xp_gain", 0.10D),
        "minecraft:cooked_cod", "minecraft:melon_slice", "minecraft:cookie");
    capstone("combo_war_scholar",      map("attack_damage", 0.05D, "damage_reduction", 0.04D, "xp_gain", 0.10D),
        "minecraft:cod", "minecraft:pufferfish", "minecraft:enchanted_golden_apple");
    }

    private ComboEffectRegistry() {
    }

    public static Map<String, Double> effects(String comboId) {
        return EFFECTS.getOrDefault(comboId, Map.of());
    }

    public static boolean isCapstone(String comboId) {
        return CAPSTONE_IDS.contains(comboId);
    }

    /**
     * Returns the food item IDs required to trigger this combo.
     */
    public static Set<String> requiredFoods(String comboId) {
        return REQUIREMENTS.getOrDefault(comboId, Set.of());
    }

    /**
     * Returns the (at most 1) combo ID whose required foods are ALL present in activeFoodIds.
     * Because each food belongs to exactly one combo, the result always has 0 or 1 elements.
     */
    public static Set<String> activeCombos(Set<String> activeFoodIds) {
        Set<String> ids = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : REQUIREMENTS.entrySet()) {
            if (activeFoodIds.containsAll(entry.getValue())) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    private static void pair(String id, Map<String, Double> effects, String food1, String food2) {
        EFFECTS.put(id, effects);
        REQUIREMENTS.put(id, Set.of(food1, food2));
    }

    private static void capstone(String id, Map<String, Double> effects, String food1, String food2, String food3) {
        EFFECTS.put(id, effects);
        REQUIREMENTS.put(id, Set.of(food1, food2, food3));
        CAPSTONE_IDS.add(id);
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
}
