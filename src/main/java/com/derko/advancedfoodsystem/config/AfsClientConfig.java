package com.derko.advancedfoodsystem.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge CLIENT config — stored in config/advancedfoodsystem-client.toml.
 * CLIENT type means changes take effect immediately without a game restart.
 * All values here are tweakable from the in-game Mods > Config screen.
 */
public final class AfsClientConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_BUFF_HUD;
    public static final ModConfigSpec.ConfigValue<String> HUD_POSITION;
    public static final ModConfigSpec.DoubleValue HUD_SCALE;
    public static final ModConfigSpec.IntValue HUD_OFFSET_X;
    public static final ModConfigSpec.IntValue HUD_OFFSET_Y;

    public static final ModConfigSpec.BooleanValue SHOW_BUFF_APPLIED;
    public static final ModConfigSpec.BooleanValue SHOW_BUFF_EXPIRED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("HUD overlay display settings (client-side — no restart needed)").push("hud");
        ENABLE_BUFF_HUD = b
                .comment("Show the active food buff HUD overlay on screen.")
                .define("enableBuffHud", true);
        HUD_POSITION = b
                .comment("Corner position of the HUD. Valid: bottom_right, bottom_left, top_right, top_left.")
                .define("position", "bottom_right");
        HUD_SCALE = b
                .comment("Scale of the HUD overlay (0.5 to 2.0).")
                .defineInRange("scale", 0.9, 0.5, 2.0);
        HUD_OFFSET_X = b
                .comment("Horizontal pixel offset inward from the selected corner.")
                .defineInRange("offsetX", 10, -500, 500);
        HUD_OFFSET_Y = b
                .comment("Vertical offset. Negative moves the HUD upward from the bottom corners.")
                .defineInRange("offsetY", -70, -500, 500);
        b.pop();

        b.comment("Action bar notification settings").push("notifications");
        SHOW_BUFF_APPLIED = b
                .comment("Show an action bar message when a food buff is applied.")
                .define("showBuffApplied", true);
        SHOW_BUFF_EXPIRED = b
                .comment("Show an action bar message when a food buff expires.")
                .define("showBuffExpired", false);
        b.pop();

        SPEC = b.build();
    }

    private AfsClientConfig() {}
}
