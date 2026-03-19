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
    private static final List<String> VALID_POSITIONS = List.of("bottom_left", "bottom_right", "top_left", "top_right");

    private static ModConfigData modConfig = new ModConfigData();
    private static Map<String, FoodBuffEntry> foodBuffs = new HashMap<>();
    private static Map<String, ComboEntry> comboBuffs = new HashMap<>();

    private static final Path BASE_DIR = FMLPaths.CONFIGDIR.get().resolve("advancedfoodsystem");
    private static final Path FOOD_CONFIG = BASE_DIR.resolve("food_buffs.json");
    private static final Path COMBO_CONFIG = BASE_DIR.resolve("buff_combinations.json");

    private ConfigManager() {}

    public static ModConfigData modConfig() { return modConfig; }
    public static Map<String, FoodBuffEntry> foodBuffs() { return foodBuffs; }
    public static Map<String, ComboEntry> comboBuffs() { return comboBuffs; }

    public static synchronized void loadOrCreate() {
        try {
            Files.createDirectories(BASE_DIR);

            if (!Files.exists(FOOD_CONFIG)) {
                writeJson(FOOD_CONFIG, defaultFoodBuffs());
            }
            if (!Files.exists(COMBO_CONFIG)) {
                writeJson(COMBO_CONFIG, defaultCombinations());
            }

            foodBuffs = readJson(FOOD_CONFIG, FOOD_MAP_TYPE, defaultFoodBuffs());
            comboBuffs = readJson(COMBO_CONFIG, COMBO_MAP_TYPE, defaultCombinations());

            sanitize();
            refreshFromNeoForgeConfigs();
        } catch (Exception ignored) {
            modConfig = new ModConfigData();
            foodBuffs = defaultFoodBuffs();
            comboBuffs = defaultCombinations();
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
            entry.healthBonusHearts = clamp(entry.healthBonusHearts, 0.0D, 6.0D);
            if (entry.buffs == null) entry.buffs = List.of();
        }

        for (ComboEntry entry : comboBuffs.values()) {
            entry.durationSeconds = clamp(entry.durationSeconds, 15, 7200);
            entry.magnitude = clamp(entry.magnitude, 0.1D, 5.0D);
            if (entry.requires == null) entry.requires = List.of();
            if (entry.buffId == null) entry.buffId = "";
        }
    }

    private static Map<String, FoodBuffEntry> defaultFoodBuffs() {
        Map<String, FoodBuffEntry> map = new HashMap<>();
        // Hearts: beef/porkchop = 1.0 (combat heavyweights), chicken = 0.75 (mid-tier attack food),
        // salmon = 0.75 (regen), golden_carrot = 1.5 (premium). Others = 0.5.
        // 3 slots Ã— best foods â†’ max 3 hearts from food (base 6 + 3 = 9). Combo adds the last to 10.
        map.put("minecraft:cooked_beef",     food(List.of("mining_speed"),                     1200, 0.25, 1.00));
        map.put("minecraft:cooked_chicken",  food(List.of("attack_speed"),                     1200, 0.15, 0.75));
        map.put("minecraft:cooked_porkchop", food(List.of("damage_reduction"),                 1200, 0.10, 1.00));
        map.put("minecraft:cooked_salmon",   food(List.of("regeneration"),                     1200, 0.25, 0.75));
        map.put("minecraft:golden_carrot",   food(List.of("regeneration", "damage_reduction"), 1200, 0.30, 1.50));
        map.put("minecraft:carrot",          food(List.of("walk_speed"),                       1200, 0.15, 0.50));
        map.put("minecraft:apple",           food(List.of("jump_height"),                      1200, 0.20, 0.50));
        map.put("minecraft:bread",           food(List.of("saturation_boost"),                 1200, 0.35, 0.50));
        map.put("minecraft:melon_slice",     food(List.of("knockback_resistance"),             1200, 0.20, 0.50));
        return map;
    }

    private static Map<String, ComboEntry> defaultCombinations() {
        Map<String, ComboEntry> map = new HashMap<>();
        // Warrior combo: beef (mining_speed) + carrot (walk_speed) â†’ grants the final heart to reach 10
        map.put("warrior_mode", combo(List.of("mining_speed", "walk_speed"), "warrior_boost", 1200, 1.5));
        return map;
    }

    private static FoodBuffEntry food(List<String> buffs, int durationSeconds, double magnitude, double healthBonusHearts) {
        FoodBuffEntry entry = new FoodBuffEntry();
        entry.buffs = buffs;
        entry.durationSeconds = durationSeconds;
        entry.magnitude = magnitude;
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
