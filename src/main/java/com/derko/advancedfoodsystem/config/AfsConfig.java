package com.derko.advancedfoodsystem.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge COMMON config (gameplay values) — stored in config/advancedfoodsystem-common.toml.
 * Requires a restart to take effect. Visible in the in-game Mods > Config screen.
 *
 * HUD/display settings that can update without restart are in {@link AfsClientConfig}.
 */
public final class AfsConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_HEARTS;
    public static final ModConfigSpec.IntValue MAX_HEARTS_WITH_FOOD;
    public static final ModConfigSpec.DoubleValue BUFF_DURATION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BUFF_MAGNITUDE_MULTIPLIER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Gameplay settings for the Advanced Food System (server/common — requires restart to change)").push("system");
        MAX_HEARTS = b
                .comment("Base maximum hearts with no food buffs active (default: 6).")
                .defineInRange("maxHearts", 6, 1, 10);
        MAX_HEARTS_WITH_FOOD = b
                .comment("Maximum hearts that food buffs can raise you to (default: 10).",
                         "The last heart above base+3 requires the Warrior combo to be active.")
                .defineInRange("maxHeartsWithFood", 10, 2, 20);
        BUFF_DURATION_MULTIPLIER = b
                .comment("Global multiplier for all food buff durations.",
                         "1.0 = default (20 min), 0.5 = 10 min, 2.0 = 40 min.")
                .defineInRange("buffDurationMultiplier", 1.0, 0.1, 10.0);
        BUFF_MAGNITUDE_MULTIPLIER = b
                .comment("Global multiplier for all food buff effect strengths.",
                         "1.0 = default, 0.5 = half strength, 2.0 = doubled.")
                .defineInRange("buffMagnitudeMultiplier", 1.0, 0.1, 5.0);
        b.pop();

        SPEC = b.build();
    }

    private AfsConfig() {}
}

