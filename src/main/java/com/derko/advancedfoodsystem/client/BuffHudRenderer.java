package com.derko.advancedfoodsystem.client;

import com.derko.advancedfoodsystem.config.ConfigManager;
import com.derko.advancedfoodsystem.data.BuffInstance;
import com.derko.advancedfoodsystem.gameplay.BuffNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/**
 * Compact food buff HUD â€” bottom-right corner.
 *
 * Layout (per slot, 80Ã—26 px):
 *   [food icon 16Ã—16] | [buff name]   [time text]
 *                       [progress bar 46Ã—3]
 *
 * Above all slots: heart-symbol row showing current bonus hearts (â™¥ filled / â™¡ empty).
 *
 * Hover over a slot â†’ tooltip with icon, name, hearts, strength, time.
 * Hover over combo badge (*) â†’ tooltip with combo details.
 *
 * Mouse coordinate fix: all hit-detection is done in HUD-scaled space to match drawn positions.
 */
public final class BuffHudRenderer {

    // Slot metrics
    private static final int SLOT_W  = 80;
    private static final int SLOT_H  = 26;
    private static final int SLOT_GAP = 2;
    private static final int ICON_X_OFF = 2;   // icon left edge relative to slot
    private static final int ICON_Y_OFF = 5;   // icon top edge relative to slot (16px icon centred in 26px)
    private static final int TEXT_X = 21;       // text left edge

    // Colours
    private static final int COL_BG        = 0xCC1A1A1A;
    private static final int COL_BORDER    = 0xFF555555;
    private static final int COL_TIME      = 0xFF55FF55;
    private static final int COL_BAR_BG    = 0xFF333333;
    private static final int COL_BAR_FG    = 0xFF00CC44;
    private static final int COL_BAR_WARN  = 0xFFFFAA00;
    private static final int COL_BAR_CRIT  = 0xFFFF3333;
    private static final int COL_HEART_ON  = 0xFFFF4444;
    private static final int COL_HEART_OFF = 0xFF553333;
    private static final int COL_TOOLTIP   = 0xCC131313;

    private static int frameCounter = 0;

    private BuffHudRenderer() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!ConfigManager.modConfig().system.enableBuffHud) return;

        frameCounter++;
        if (frameCounter % ConfigManager.modConfig().hud.renderFrequency != 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        List<BuffInstance> all = ClientBuffState.get();
        if (all.isEmpty()) return;

        List<BuffInstance> foodBuffs = all.stream().filter(b -> !isCombo(b)).toList();
        boolean comboActive = all.stream().anyMatch(BuffHudRenderer::isCombo);
        if (foodBuffs.isEmpty() && !comboActive) return;

        int max = Math.min(3, foodBuffs.size());
        List<BuffInstance> show = foodBuffs.stream()
                .sorted((a, b) -> Long.compare(a.created(), b.created()))
                .limit(max).toList();

        GuiGraphics gfx = event.getGuiGraphics();
        Font font = mc.font;

        float scale = (float) ConfigManager.modConfig().hud.scale;
        gfx.pose().pushPose();
        gfx.pose().scale(scale, scale, 1.0F);

        // Coordinate space: divide GUI resolution by scale to get HUD-space resolution
        int screenW = (int) (mc.getWindow().getGuiScaledWidth()  / scale);
        int screenH = (int) (mc.getWindow().getGuiScaledHeight() / scale);

        int ox = ConfigManager.modConfig().hud.offsetX;
        int oy = ConfigManager.modConfig().hud.offsetY;
        String position = ConfigManager.modConfig().hud.position;

        int stackHeight = max > 0 ? (max * SLOT_H) + ((max - 1) * SLOT_GAP) : SLOT_H;

        int x;
        int y;

        switch (position) {
            case "bottom_left" -> {
                x = ox;
                y = screenH - stackHeight + oy;
            }
            case "top_left" -> {
                x = ox;
                y = oy;
            }
            case "top_right" -> {
                x = screenW - SLOT_W - ox;
                y = oy;
            }
            default -> {
                x = screenW - SLOT_W - ox;
                y = screenH - stackHeight + oy;
            }
        }

        // Keep the full stack and heart summary on-screen, even with aggressive offsets.
        x = clamp(x, 2, Math.max(2, screenW - SLOT_W - 2));
        y = clamp(y, 16, Math.max(16, screenH - stackHeight - 2));

        // --- Heart summary row (above slots) ---
        drawHeartSummary(gfx, font, x, y - 14, all);

        // --- Food buff slots ---
        for (int i = 0; i < show.size(); i++) {
            int sy = y + i * (SLOT_H + SLOT_GAP);
            drawSlot(gfx, font, show.get(i), x, sy, screenW, screenH);
        }

        // --- Combo badge ---
        if (comboActive) {
            BuffInstance comboBuff = all.stream().filter(BuffHudRenderer::isCombo).findFirst().orElse(null);
            int badgeX = x - 14;
            int badgeY = y;
            drawComboBadge(gfx, font, badgeX, badgeY, comboBuff, screenW, screenH);
        }

        gfx.pose().popPose();
    }

    // -------------------------------------------------------------------------
    // Heart summary
    // -------------------------------------------------------------------------

    private static void drawHeartSummary(GuiGraphics gfx, Font font, int x, int y, List<BuffInstance> all) {
        double baseHearts = ConfigManager.modConfig().system.maxHearts;
        double maxHearts  = ConfigManager.modConfig().system.maxHeartsWithFood;

        double slotHearts = all.stream().filter(b -> !isCombo(b))
                .mapToDouble(BuffInstance::healthBonusHearts).sum();
        boolean comboActive = all.stream().anyMatch(BuffHudRenderer::isCombo);

        double effectiveHearts = comboActive
                ? maxHearts
                : Math.min(maxHearts - 1.0D, baseHearts + slotHearts);
        double extraHearts = Math.max(0.0D, effectiveHearts - baseHearts);
        int maxBonus = (int) Math.round(maxHearts - baseHearts); // = 4

        // Build heart string: filled â™¥ for active bonus, hollow â™¡ for remaining potential
        int filled = (int) Math.ceil(extraHearts);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxBonus; i++) {
            sb.append(i < filled ? "\u2665" : "\u2661"); // â™¥ or â™¡
        }
        String hearts = sb.toString();

        // Small framed label
        int tw = font.width(hearts) + 8;
        int th = 10;
        int lx = x + SLOT_W - tw; // right-align with slots

        gfx.fill(lx, y, lx + tw, y + th, COL_BG);
        gfx.fill(lx, y, lx + tw, y + 1, COL_BORDER);
        gfx.fill(lx, y + th - 1, lx + tw, y + th, COL_BORDER);
        gfx.fill(lx, y, lx + 1, y + th, COL_BORDER);
        gfx.fill(lx + tw - 1, y, lx + tw, y + th, COL_BORDER);
        // Draw each heart character in its colour
        int cx = lx + 4;
        for (int i = 0; i < maxBonus; i++) {
            String ch = i < filled ? "\u2665" : "\u2661";
            int colour = i < filled ? COL_HEART_ON : COL_HEART_OFF;
            gfx.drawString(font, ch, cx, y + 1, colour, false);
            cx += font.width(ch);
        }
    }

    // -------------------------------------------------------------------------
    // Slot
    // -------------------------------------------------------------------------

    private static void drawSlot(GuiGraphics gfx, Font font, BuffInstance buff,
                                 int x, int y, int screenW, int screenH) {
        // Background + border
        gfx.fill(x, y, x + SLOT_W, y + SLOT_H, COL_BG);
        gfx.fill(x, y, x + SLOT_W, y + 1, COL_BORDER);
        gfx.fill(x, y + SLOT_H - 1, x + SLOT_W, y + SLOT_H, COL_BORDER);
        gfx.fill(x, y, x + 1, y + SLOT_H, COL_BORDER);
        gfx.fill(x + SLOT_W - 1, y, x + SLOT_W, y + SLOT_H, COL_BORDER);

        // Food icon
        drawFoodIcon(gfx, buff.source(), x + ICON_X_OFF, y + ICON_Y_OFF);

        // Time text (above bar)
        String time = formatShortTime(buff.timeTicks());
        gfx.drawString(font, time, x + TEXT_X, y + 4, COL_TIME, false);

        // Progress bar
        int barX = x + TEXT_X;
        int barY = y + SLOT_H - 7;
        int barW = SLOT_W - TEXT_X - 4;
        double frac = buff.totalTicks() > 0
                ? Math.max(0.0, buff.timeTicks() / (double) buff.totalTicks())
                : 0.0;
        int filled = (int) Math.round(frac * barW);
        int barColour = frac > 0.25 ? COL_BAR_FG : (frac > 0.10 ? COL_BAR_WARN : COL_BAR_CRIT);

        gfx.fill(barX, barY, barX + barW, barY + 3, COL_BAR_BG);
        if (filled > 0) {
            gfx.fill(barX, barY, barX + filled, barY + 3, barColour);
        }

        // Hover tooltip â€” must be in HUD-scaled mouse space
        maybeDrawSlotTooltip(gfx, font, buff, x, y, SLOT_W, SLOT_H, screenW, screenH);
    }

    // -------------------------------------------------------------------------
    // Combo badge
    // -------------------------------------------------------------------------

    private static void drawComboBadge(GuiGraphics gfx, Font font,
                                       int x, int y, BuffInstance comboBuff,
                                       int screenW, int screenH) {
        int bw = 12, bh = 12;
        gfx.fill(x, y, x + bw, y + bh, COL_BG);
        gfx.fill(x, y, x + bw, y + 1, 0xFFAA8800);
        gfx.fill(x, y + bh - 1, x + bw, y + bh, 0xFFAA8800);
        gfx.fill(x, y, x + 1, y + bh, 0xFFAA8800);
        gfx.fill(x + bw - 1, y, x + bw, y + bh, 0xFFAA8800);
        gfx.drawString(font, "\u2605", x + 2, y + 2, 0xFFFFC857, false); // â˜…

        if (comboBuff != null) {
            maybeDrawComboTooltip(gfx, font, x, y, bw, bh, comboBuff, screenW, screenH);
        }
    }

    // -------------------------------------------------------------------------
    // Hover tooltips
    // -------------------------------------------------------------------------

    private static void maybeDrawSlotTooltip(GuiGraphics gfx, Font font,
                                             BuffInstance buff,
                                             int x, int y, int w, int h,
                                             int screenW, int screenH) {
        int[] mouse = scaledMouse();
        if (mouse[0] < x || mouse[0] > x + w || mouse[1] < y || mouse[1] > y + h) return;

        String buffName  = BuffNames.pretty(buff.id());
        String icon      = BuffNames.icon(buff.id());
        String heartStr  = formatHearts(buff.healthBonusHearts()) + "\u2665";
        String magStr    = "Strength x" + String.format("%.2f", buff.magnitude());
        String timeStr   = formatShortTime(buff.timeTicks()) + " remaining";
        String sourceStr = "Food: " + friendlyId(buff.source());

        String title = icon + " " + buffName + "  +" + heartStr;
        int tw = maxWidth(font, title, magStr, timeStr, sourceStr) + 8;
        int th = 42;
        int tx = resolveTooltipX(x, w, tw, screenW);
        int ty = resolveTooltipY(y, th, screenH);

        drawTooltipBox(gfx, tx, ty, tw, th);
        gfx.drawString(font, title,     tx + 4, ty + 3,  0xFFFFFFFF, false);
        gfx.drawString(font, magStr,    tx + 4, ty + 13, 0xFFAAAAAA, false);
        gfx.drawString(font, timeStr,   tx + 4, ty + 23, COL_TIME,   false);
        gfx.drawString(font, sourceStr, tx + 4, ty + 33, 0xFF888888, false);
    }

    private static void maybeDrawComboTooltip(GuiGraphics gfx, Font font,
                                              int x, int y, int w, int h,
                                              BuffInstance comboBuff,
                                              int screenW, int screenH) {
        int[] mouse = scaledMouse();
        if (mouse[0] < x || mouse[0] > x + w || mouse[1] < y || mouse[1] > y + h) return;

        String title    = "\u2605 Warrior Combo";
        String requires = "Requires: Beef + Carrot";
        String bonus    = "Grants final heart (+1\u2665 to reach 10)";
        String timeStr  = formatShortTime(comboBuff.timeTicks()) + " remaining";

        int tw = maxWidth(font, title, requires, bonus, timeStr) + 8;
        int th = 42;
        int tx = resolveTooltipX(x, w, tw, screenW);
        int ty = resolveTooltipY(y, th, screenH);

        drawTooltipBox(gfx, tx, ty, tw, th);
        gfx.drawString(font, title,    tx + 4, ty + 3,  0xFFFFC857, false);
        gfx.drawString(font, requires, tx + 4, ty + 13, 0xFFAAAAAA, false);
        gfx.drawString(font, bonus,    tx + 4, ty + 23, COL_HEART_ON, false);
        gfx.drawString(font, timeStr,  tx + 4, ty + 33, COL_TIME,   false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns mouse position in HUD-scaled coordinate space.
     * Slot positions are computed with screenW = guiScaled / hudScale, so mouse must
     * be transformed the same way for hit-detection to work correctly.
     */
    private static int[] scaledMouse() {
        Minecraft mc = Minecraft.getInstance();
        double hudScale = ConfigManager.modConfig().hud.scale;
        int mx = (int) Math.round(
                mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                / (mc.getWindow().getScreenWidth() * hudScale));
        int my = (int) Math.round(
                mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                / (mc.getWindow().getScreenHeight() * hudScale));
        return new int[]{mx, my};
    }

    private static int resolveTooltipX(int anchorX, int anchorW, int tooltipW, int screenW) {
        int right = anchorX + anchorW + 4;
        if (right + tooltipW <= screenW - 2) return right;
        return Math.max(2, anchorX - tooltipW - 4);
    }

    private static int resolveTooltipY(int anchorY, int tooltipH, int screenH) {
        if (anchorY + tooltipH <= screenH - 2) return anchorY;
        return Math.max(2, anchorY - tooltipH - 4);
    }

    private static void drawTooltipBox(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, COL_TOOLTIP);
        gfx.fill(x, y, x + w, y + 1, COL_BORDER);
        gfx.fill(x, y + h - 1, x + w, y + h, COL_BORDER);
        gfx.fill(x, y, x + 1, y + h, COL_BORDER);
        gfx.fill(x + w - 1, y, x + w, y + h, COL_BORDER);
    }

    private static void drawFoodIcon(GuiGraphics gfx, String source, int x, int y) {
        try {
            ResourceLocation id = ResourceLocation.parse(source);
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null && item != BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
                gfx.renderItem(new ItemStack(item), x, y);
                return;
            }
        } catch (Exception ignored) {}
        gfx.drawString(Minecraft.getInstance().font, "?", x + 4, y + 4, 0xFFFFFFFF, false);
    }

    private static int maxWidth(Font font, String... strings) {
        int max = 0;
        for (String s : strings) max = Math.max(max, font.width(s));
        return max;
    }

    private static String formatShortTime(int ticks) {
        int sec = Math.max(0, ticks / 20);
        int min = sec / 60;
        int rem = sec % 60;
        if (min >= 2) return min + "m";
        if (min == 1) return rem == 0 ? "1m" : "1m " + rem + "s";
        return rem + "s";
    }

    private static String formatHearts(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) return Integer.toString((int) Math.rint(value));
        return String.format("%.1f", value);
    }

    private static String friendlyId(String source) {
        // Turn "minecraft:cooked_beef" â†’ "Cooked Beef"
        try {
            String path = ResourceLocation.parse(source).getPath();
            String[] parts = path.split("_");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!sb.isEmpty()) sb.append(' ');
                if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
            return sb.toString();
        } catch (Exception e) {
            return source;
        }
    }

    private static boolean isCombo(BuffInstance buff) {
        return buff.source().startsWith("combo:");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
