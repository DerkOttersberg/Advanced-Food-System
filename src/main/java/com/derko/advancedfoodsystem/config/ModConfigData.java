package com.derko.advancedfoodsystem.config;

public class ModConfigData {
    public SystemSettings system = new SystemSettings();
    public HudSettings hud = new HudSettings();
    public NotificationSettings notifications = new NotificationSettings();

    public static class SystemSettings {
        public int maxHearts = 6;
        public int maxHeartsWithFood = 10;
        public boolean enableBuffHud = true;
        public int maxActiveBuffs = 3;
        public double buffDurationMultiplier = 1.0D;
        public double buffMagnitudeMultiplier = 1.0D;
    }

    public static class HudSettings {
        public String position = "bottom_right";
        public double scale = 0.9D;
        public int maxBuffsShown = 3;
        public int offsetX = 10;
        public int offsetY = -70;
        public int renderFrequency = 1;
    }

    public static class NotificationSettings {
        public boolean showBuffApplied = true;
        public boolean showBuffExpired = false;
    }
}
