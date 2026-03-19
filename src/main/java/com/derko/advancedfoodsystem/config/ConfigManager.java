package com.derko.advancedfoodsystem.config;

import com.derko.seamlessapi.SatiationAPI;
import com.derko.seamlessapi.api.FoodBuffRegistration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Type FOOD_MAP_TYPE = new TypeToken<Map<String, FoodBuffEntry>>() {}.getType();
    private static final Type COMBO_MAP_TYPE = new TypeToken<Map<String, ComboEntry>>() {}.getType();
    private static final Type EFFECT_STRENGTH_TYPE = new TypeToken<Map<String, Double>>() {}.getType();
    private static final List<String> VALID_POSITIONS = List.of("bottom_left", "bottom_right", "top_left", "top_right");
    private static final List<String> BALANCED_ONE_HEART_FOODS = List.of(
            "minecraft:cooked_beef",
            "minecraft:cooked_chicken",
            "minecraft:cooked_porkchop",
            "minecraft:cooked_salmon",
            "minecraft:golden_carrot",
            "minecraft:carrot",
            "minecraft:apple",
            "minecraft:bread",
            "minecraft:melon_slice"
        );

    private static ModConfigData modConfig = new ModConfigData();
    private static Map<String, FoodBuffEntry> foodBuffs = new HashMap<>();
    private static Map<String, ComboEntry> comboBuffs = new HashMap<>();
    private static Map<String, Double> effectStrengths = new HashMap<>();
    private static boolean hungerDebugEnabled = false;

    private static final Path BASE_DIR = FMLPaths.CONFIGDIR.get().resolve("advancedfoodsystem");
    private static final Path FOOD_CONFIG = BASE_DIR.resolve("food_buffs.json");
    private static final Path COMBO_CONFIG = BASE_DIR.resolve("buff_combinations.json");
    private static final Path EFFECT_STRENGTH_CONFIG = BASE_DIR.resolve("effect_strengths.json");

    private ConfigManager() {}

    public static ModConfigData modConfig() { return modConfig; }
    public static Map<String, FoodBuffEntry> foodBuffs() { return foodBuffs; }
    public static Map<String, ComboEntry> comboBuffs() { return comboBuffs; }
    public static synchronized Map<String, Double> effectStrengths() { return new HashMap<>(effectStrengths); }

    public static synchronized List<String> effectStrengthKeys() {
        return effectStrengths.keySet().stream().sorted().toList();
    }

    public static synchronized boolean isHungerDebugEnabled() {
        return hungerDebugEnabled;
    }

    public static synchronized void setHungerDebugEnabled(boolean enabled) {
        hungerDebugEnabled = enabled;
    }

    public static synchronized double effectStrengthMultiplier(String buffId) {
        return effectStrengths.getOrDefault(buffId, 1.0D);
    }

    public static synchronized void setEffectStrengthMultiplier(String buffId, double value) {
        if (buffId == null || buffId.isBlank()) {
            return;
        }
        effectStrengths.put(buffId, clamp(value, 0.0D, 5.0D));
    }

    public static synchronized void saveEffectStrengths() {
        try {
            writeJson(EFFECT_STRENGTH_CONFIG, effectStrengths);
        } catch (IOException ignored) {
        }
    }

    public static synchronized void loadOrCreate() {
        try {
            Files.createDirectories(BASE_DIR);

            if (!Files.exists(FOOD_CONFIG)) {
                writeJson(FOOD_CONFIG, defaultFoodBuffs());
            }
            if (!Files.exists(COMBO_CONFIG)) {
                writeJson(COMBO_CONFIG, defaultCombinations());
            }
            if (!Files.exists(EFFECT_STRENGTH_CONFIG)) {
                writeJson(EFFECT_STRENGTH_CONFIG, defaultEffectStrengths());
            }

            foodBuffs = readJson(FOOD_CONFIG, FOOD_MAP_TYPE, defaultFoodBuffs());
            comboBuffs = readJson(COMBO_CONFIG, COMBO_MAP_TYPE, defaultCombinations());
            effectStrengths = readJson(EFFECT_STRENGTH_CONFIG, EFFECT_STRENGTH_TYPE, defaultEffectStrengths());

            mergeMissingDefaultFoods();

            sanitize();
            refreshFromNeoForgeConfigs();
        } catch (Exception ignored) {
            modConfig = new ModConfigData();
            foodBuffs = defaultFoodBuffs();
            comboBuffs = defaultCombinations();
            effectStrengths = defaultEffectStrengths();
        }
    }

    private static void mergeMissingDefaultFoods() {
        Map<String, FoodBuffEntry> defaults = defaultFoodBuffs();
        boolean changed = false;

        for (Map.Entry<String, FoodBuffEntry> defaultEntry : defaults.entrySet()) {
            FoodBuffEntry existing = foodBuffs.get(defaultEntry.getKey());
            if (existing == null) {
                foodBuffs.put(defaultEntry.getKey(), defaultEntry.getValue());
                changed = true;
                continue;
            }

            FoodBuffEntry def = defaultEntry.getValue();
            if ((existing.tags == null || existing.tags.isEmpty()) && def.tags != null && !def.tags.isEmpty()) {
                existing.tags = new ArrayList<>(def.tags);
                changed = true;
            }
            if ((existing.debuffs == null || existing.debuffs.isEmpty()) && def.debuffs != null && !def.debuffs.isEmpty()) {
                existing.debuffs = new ArrayList<>(def.debuffs);
                existing.debuffMagnitude = def.debuffMagnitude;
                changed = true;
            }
            if (existing.healthBonusHearts <= 0.0D && def.healthBonusHearts > 0.0D) {
                existing.healthBonusHearts = def.healthBonusHearts;
                changed = true;
            }
        }

        if (changed) {
            try {
                writeJson(FOOD_CONFIG, foodBuffs);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Called by AdvancedFoodSystemMod after FMLLoadCompleteEvent so all API mods have registered.
     * Merges third-party food registrations into the loaded food buff map.
     * JSON configs take priority over API registrations (existing key = skip API entry).
     */
    public static synchronized void mergeApiRegistrations(Map<String, FoodBuffRegistration> apiEntries) {
        for (Map.Entry<String, FoodBuffRegistration> e : apiEntries.entrySet()) {
            if (foodBuffs.containsKey(e.getKey())) {
                continue; // JSON config takes priority
            }
            FoodBuffRegistration reg = e.getValue();
            FoodBuffEntry entry = new FoodBuffEntry();
            entry.buffs = new ArrayList<>(reg.buffs());
            entry.durationSeconds = clamp(reg.durationSeconds(), 15, 7200);
            entry.magnitude = clamp(reg.magnitude(), 0.01D, 5.0D);
            entry.healthBonusHearts = clamp(reg.healthBonusHearts(), 0.0D, 6.0D);
            foodBuffs.put(e.getKey(), entry);
        }
    }

    /**
     * Reads live values from AfsConfig (COMMON) and AfsClientConfig (CLIENT).
     * Called on config load/reload events and at mod startup.
     * Safe to call before configs are loaded â€” silently ignored if not ready.
     */
    public static synchronized void refreshFromNeoForgeConfigs() {
        try {
            modConfig.system.maxHearts = AfsConfig.MAX_HEARTS.get();
            modConfig.system.maxHeartsWithFood = AfsConfig.MAX_HEARTS_WITH_FOOD.get();
            modConfig.system.wholeHeartHealthScaling = AfsConfig.WHOLE_HEART_HEALTH_SCALING.get();
            modConfig.system.buffDurationMultiplier = AfsConfig.BUFF_DURATION_MULTIPLIER.get();
            modConfig.system.buffMagnitudeMultiplier = AfsConfig.BUFF_MAGNITUDE_MULTIPLIER.get();
        } catch (Exception ignored) { /* COMMON config not yet loaded */ }

        try {
            modConfig.system.enableBuffHud = AfsClientConfig.ENABLE_BUFF_HUD.get();

            String pos = AfsClientConfig.HUD_POSITION.get();
            if (VALID_POSITIONS.contains(pos)) {
                modConfig.hud.position = pos;
            }
            modConfig.hud.scale = AfsClientConfig.HUD_SCALE.get();
            modConfig.hud.offsetX = AfsClientConfig.HUD_OFFSET_X.get();
            modConfig.hud.offsetY = AfsClientConfig.HUD_OFFSET_Y.get();

            modConfig.notifications.showBuffApplied = AfsClientConfig.SHOW_BUFF_APPLIED.get();
            modConfig.notifications.showBuffExpired = AfsClientConfig.SHOW_BUFF_EXPIRED.get();
        } catch (Exception ignored) { /* CLIENT config not yet loaded */ }
    }

    // Legacy alias used by old code paths â€” delegates to new name
    public static void refreshFromAfsConfig() {
        refreshFromNeoForgeConfigs();
    }

    private static void sanitize() {
        modConfig.system.maxActiveBuffs = 3;
        modConfig.hud.maxBuffsShown = 3;
        modConfig.hud.renderFrequency = clamp(modConfig.hud.renderFrequency, 1, 10);

        for (FoodBuffEntry entry : foodBuffs.values()) {
            entry.durationSeconds = clamp(entry.durationSeconds, 15, 7200);
            entry.magnitude = clamp(entry.magnitude, 0.01D, 5.0D);
            entry.debuffMagnitude = clamp(entry.debuffMagnitude, 0.0D, 1.0D);
            entry.healthBonusHearts = clamp(entry.healthBonusHearts, 0.0D, 6.0D);
            if (entry.buffs == null) {
                entry.buffs = List.of();
            } else {
                entry.buffs = entry.buffs.stream()
                        .map(buff -> "jump_height".equals(buff) ? "walk_speed" : buff)
                        .toList();
            }
            if (entry.debuffs == null) {
                entry.debuffs = List.of();
            }
            if (entry.tags == null) {
                entry.tags = List.of();
            }
        }

        // Migration: keep bundled vanilla defaults at +1 heart after rebalancing.
        for (String foodId : BALANCED_ONE_HEART_FOODS) {
            FoodBuffEntry entry = foodBuffs.get(foodId);
            if (entry != null && entry.healthBonusHearts > 0.0D) {
                entry.healthBonusHearts = 1.0D;
            }
        }

        // Migration: switch bread from old saturation_boost identity to hunger_efficiency.
        FoodBuffEntry bread = foodBuffs.get("minecraft:bread");
        if (bread != null && bread.buffs != null && !bread.buffs.isEmpty()) {
            bread.buffs = bread.buffs.stream()
                    .map(buff -> "saturation_boost".equals(buff) ? "hunger_efficiency" : buff)
                    .toList();
        }

        for (ComboEntry entry : comboBuffs.values()) {
            entry.durationSeconds = clamp(entry.durationSeconds, 15, 7200);
            entry.magnitude = clamp(entry.magnitude, 0.1D, 5.0D);
            if (entry.requires == null) entry.requires = List.of();
            if (entry.buffId == null) entry.buffId = "";
        }

        for (String buffId : discoverAllBuffIds()) {
            effectStrengths.putIfAbsent(buffId, 1.0D);
        }
        effectStrengths.replaceAll((id, value) -> clamp(value == null ? 1.0D : value, 0.0D, 5.0D));
    }

    private static List<String> discoverAllBuffIds() {
        List<String> ids = new ArrayList<>();
        for (FoodBuffEntry entry : foodBuffs.values()) {
            if (entry.buffs != null) {
                for (String id : entry.buffs) {
                    if (id != null && !id.isBlank() && !ids.contains(id)) {
                        ids.add(id);
                    }
                }
            }
            if (entry.debuffs != null) {
                for (String id : entry.debuffs) {
                    if (id != null && !id.isBlank() && !ids.contains(id)) {
                        ids.add(id);
                    }
                }
            }
        }

        for (ComboEntry combo : comboBuffs.values()) {
            if (combo.buffId != null && !combo.buffId.isBlank() && !ids.contains(combo.buffId)) {
                ids.add(combo.buffId);
            }
        }

        return ids;
    }

    private static Map<String, FoodBuffEntry> defaultFoodBuffs() {
        Map<String, FoodBuffEntry> map = new HashMap<>();
        // Complete baseline matrix (all 40 edible foods); 20 minute base effects/debuffs.
        map.put("minecraft:apple", food(List.of("regeneration"), List.of(), List.of("S"), 1200, 0.10, 0.04, 1.0));
        map.put("minecraft:mushroom_stew", food(List.of("heart_bonus"), List.of(), List.of("N"), 1200, 0.01, 0.04, 1.0));
        map.put("minecraft:bread", food(List.of("hunger_efficiency"), List.of(), List.of("U"), 1200, 0.18, 0.04, 1.0));
        map.put("minecraft:porkchop", food(List.of("attack_damage"), List.of("frailty"), List.of("O", "R"), 1200, 0.07, 0.04, 1.0));
        map.put("minecraft:cooked_porkchop", food(List.of("damage_reduction"), List.of(), List.of("D"), 1200, 0.12, 0.04, 1.0));
        map.put("minecraft:golden_apple", food(List.of("regeneration", "damage_reduction"), List.of(), List.of("S", "D"), 1200, 0.18, 0.04, 1.0));
        map.put("minecraft:enchanted_golden_apple", food(List.of("regeneration", "damage_reduction", "xp_gain"), List.of(), List.of("S", "D", "U"), 1200, 0.12, 0.04, 1.0));
        map.put("minecraft:cod", food(List.of("xp_gain"), List.of(), List.of("U"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:salmon", food(List.of("regeneration", "xp_gain"), List.of(), List.of("S", "U"), 1200, 0.06, 0.04, 1.0));
        map.put("minecraft:tropical_fish", food(List.of("walk_speed", "xp_gain"), List.of(), List.of("M", "U"), 1200, 0.05, 0.04, 1.0));
        map.put("minecraft:pufferfish", food(List.of("damage_reduction", "xp_gain"), List.of("queasy"), List.of("D", "U", "R"), 1200, 0.12, 0.03, 1.0));
        map.put("minecraft:cooked_cod", food(List.of("xp_gain", "hunger_efficiency"), List.of(), List.of("U"), 1200, 0.12, 0.04, 1.0));
        map.put("minecraft:cooked_salmon", food(List.of("regeneration"), List.of(), List.of("S"), 1200, 0.14, 0.04, 1.0));
        map.put("minecraft:cookie", food(List.of("xp_gain", "mining_speed"), List.of(), List.of("U"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:melon_slice", food(List.of("knockback_resistance", "walk_speed"), List.of(), List.of("M", "D"), 1200, 0.06, 0.04, 1.0));
        map.put("minecraft:beef", food(List.of("attack_damage"), List.of("fatigue"), List.of("O", "R"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:cooked_beef", food(List.of("attack_damage", "mining_speed"), List.of(), List.of("O", "U"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:chicken", food(List.of("attack_speed"), List.of("appetite_leak"), List.of("O", "R"), 1200, 0.10, 0.06, 1.0));
        map.put("minecraft:cooked_chicken", food(List.of("attack_speed"), List.of(), List.of("O"), 1200, 0.16, 0.04, 1.0));
        map.put("minecraft:rotten_flesh", food(List.of("attack_damage", "hunger_efficiency"), List.of("frailty"), List.of("O", "U", "R"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:spider_eye", food(List.of("attack_damage", "xp_gain"), List.of("fatigue"), List.of("O", "U", "R"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:carrot", food(List.of("walk_speed"), List.of(), List.of("M"), 1200, 0.14, 0.04, 1.0));
        map.put("minecraft:potato", food(List.of("hunger_efficiency"), List.of(), List.of("U"), 1200, 0.10, 0.04, 1.0));
        map.put("minecraft:baked_potato", food(List.of("hunger_efficiency", "saturation_boost"), List.of(), List.of("U", "S"), 1200, 0.06, 0.04, 1.0));
        map.put("minecraft:poisonous_potato", food(List.of("walk_speed", "xp_gain"), List.of("frailty"), List.of("M", "U", "R"), 1200, 0.10, 0.04, 1.0));
        map.put("minecraft:golden_carrot", food(List.of("regeneration", "damage_reduction", "xp_gain"), List.of(), List.of("S", "D", "U"), 1200, 0.10, 0.04, 1.0));
        map.put("minecraft:pumpkin_pie", food(List.of("hunger_efficiency", "saturation_boost"), List.of(), List.of("U", "S"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:rabbit", food(List.of("walk_speed", "attack_damage"), List.of("appetite_leak"), List.of("M", "O", "R"), 1200, 0.05, 0.06, 1.0));
        map.put("minecraft:cooked_rabbit", food(List.of("walk_speed", "attack_damage"), List.of(), List.of("M", "O"), 1200, 0.10, 0.04, 1.0));
        map.put("minecraft:rabbit_stew", food(List.of("heart_bonus"), List.of(), List.of("N"), 1200, 0.01, 0.04, 1.0));
        map.put("minecraft:mutton", food(List.of("attack_damage"), List.of("frailty"), List.of("O", "R"), 1200, 0.06, 0.04, 1.0));
        map.put("minecraft:cooked_mutton", food(List.of("damage_reduction"), List.of(), List.of("D"), 1200, 0.10, 0.04, 1.0));
        map.put("minecraft:chorus_fruit", food(List.of("walk_speed", "knockback_resistance"), List.of(), List.of("M"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:beetroot", food(List.of("xp_gain"), List.of(), List.of("U"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:beetroot_soup", food(List.of("heart_bonus"), List.of(), List.of("N"), 1200, 0.01, 0.04, 1.0));
        map.put("minecraft:dried_kelp", food(List.of("hunger_efficiency", "mining_speed"), List.of(), List.of("U"), 1200, 0.06, 0.04, 1.0));
        map.put("minecraft:suspicious_stew", food(List.of("heart_bonus"), List.of(), List.of("N"), 1200, 0.01, 0.04, 1.0));
        map.put("minecraft:sweet_berries", food(List.of("walk_speed", "xp_gain"), List.of(), List.of("M", "U"), 1200, 0.06, 0.04, 1.0));
        map.put("minecraft:glow_berries", food(List.of("xp_gain", "mining_speed"), List.of(), List.of("U"), 1200, 0.08, 0.04, 1.0));
        map.put("minecraft:cake", food(List.of("saturation_boost", "hunger_efficiency"), List.of(), List.of("U", "S"), 1200, 0.08, 0.04, 1.0));
        return map;
    }

    private static Map<String, ComboEntry> defaultCombinations() {
        return new HashMap<>();
    }

    private static Map<String, Double> defaultEffectStrengths() {
        Map<String, Double> map = new HashMap<>();
        map.put("mining_speed", 1.0D);
        map.put("walk_speed", 1.0D);
        map.put("attack_speed", 1.0D);
        map.put("damage_reduction", 1.0D);
        map.put("regeneration", 1.0D);
        map.put("hunger_efficiency", 1.0D);
        map.put("saturation_boost", 1.0D);
        map.put("knockback_resistance", 1.0D);
        map.put("attack_damage", 1.0D);
        map.put("xp_gain", 1.0D);
        map.put("frailty", 1.0D);
        map.put("fatigue", 1.0D);
        map.put("queasy", 1.0D);
        map.put("appetite_leak", 1.0D);
        map.put("heart_bonus", 1.0D);
        return map;
    }

    private static FoodBuffEntry food(List<String> buffs, List<String> debuffs, List<String> tags,
                                      int durationSeconds, double magnitude, double debuffMagnitude, double healthBonusHearts) {
        FoodBuffEntry entry = new FoodBuffEntry();
        entry.buffs = buffs;
        entry.debuffs = debuffs;
        entry.tags = tags;
        entry.durationSeconds = durationSeconds;
        entry.magnitude = magnitude;
        entry.debuffMagnitude = debuffMagnitude;
        entry.healthBonusHearts = healthBonusHearts;
        return entry;
    }

    private static ComboEntry combo(List<String> requires, String buffId, int durationSeconds, double magnitude) {
        ComboEntry entry = new ComboEntry();
        entry.requires = requires;
        entry.buffId = buffId;
        entry.durationSeconds = durationSeconds;
        entry.magnitude = magnitude;
        return entry;
    }

    private static <T> T readJson(Path path, Class<T> clazz, T fallback) {
        try (Reader reader = Files.newBufferedReader(path)) {
            T value = GSON.fromJson(reader, clazz);
            return value == null ? fallback : value;
        } catch (IOException e) {
            return fallback;
        }
    }

    private static <T> T readJson(Path path, Type type, T fallback) {
        try (Reader reader = Files.newBufferedReader(path)) {
            T value = GSON.fromJson(reader, type);
            return value == null ? fallback : value;
        } catch (IOException e) {
            return fallback;
        }
    }

    private static void writeJson(Path path, Object object) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(object, writer);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
