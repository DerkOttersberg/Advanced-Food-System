package com.derko.advancedfoodsystem.client;

import com.derko.advancedfoodsystem.config.ConfigManager;
import com.derko.advancedfoodsystem.data.BuffInstance;
import com.derko.advancedfoodsystem.gameplay.BuffNames;
import com.derko.advancedfoodsystem.gameplay.ComboEffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    // Slot metrics tuned for compact timer-only layout.
    private static final int SLOT_W  = 52;
    private static final int SLOT_H  = 20;
    private static final int SLOT_GAP = 2;
    private static final int ICON_X_OFF = 2;
    private static final int ICON_Y_OFF = 3;
    private static final int TEXT_X = 17;

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

        List<BuffInstance> show = groupedFoodSlots(foodBuffs).stream().limit(3).toList();
        int max = show.size();

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

        ComboTooltipData comboData = comboActive ? buildComboTooltipData(all) : null;

        // --- Heart summary row (above slots) ---
        drawHeartSummary(gfx, font, x, y - 14, all, comboData, screenW, screenH);

        // --- Food buff slots ---
        for (int i = 0; i < show.size(); i++) {
            int sy = y + i * (SLOT_H + SLOT_GAP);
            drawSlot(gfx, font, show.get(i), x, sy, screenW, screenH);
        }

        gfx.pose().popPose();
    }

    // -------------------------------------------------------------------------
    // Heart summary
    // -------------------------------------------------------------------------

    private static void drawHeartSummary(GuiGraphics gfx, Font font, int x, int y, List<BuffInstance> all,
                                         ComboTooltipData comboData, int screenW, int screenH) {
        double baseHearts = ConfigManager.modConfig().system.maxHearts;
        double maxHearts  = ConfigManager.modConfig().system.maxHeartsWithFood;

        Map<String, Double> sourceHeartMap = new HashMap<>();
        for (BuffInstance buff : all) {
            if (!isCombo(buff)) {
                sourceHeartMap.merge(baseSource(buff.source()), buff.healthBonusHearts(), Math::max);
            }
        }

        double slotHearts = sourceHeartMap.values().stream().mapToDouble(Double::doubleValue).sum();
        boolean wholeHeartScaling = ConfigManager.modConfig().system.wholeHeartHealthScaling;
        double appliedSlotHearts = wholeHeartScaling ? Math.floor(slotHearts) : slotHearts;

        boolean capstoneComboActive = all.stream().anyMatch(
            buff -> isCombo(buff) && ComboEffectRegistry.grantsFinalHeart(buff.id())
        );

        double effectiveHearts = Math.min(maxHearts, baseHearts + appliedSlotHearts + (capstoneComboActive ? 1.0D : 0.0D));
        double extraHearts = Math.max(0.0D, effectiveHearts - baseHearts);
        int reachableBonus = ConfigManager.modConfig().system.maxActiveBuffs + 1;
        int maxBonus = (int) Math.round(Math.min(maxHearts - baseHearts, reachableBonus));

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

        if (comboData != null && comboData.primary() != null) {
            int badgeX = Math.max(2, lx - 14);
            int badgeY = y - 1;
            drawComboBadge(gfx, font, badgeX, badgeY, comboData, screenW, screenH);
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

        // Time text
        String time = formatShortTime(buff.timeTicks());
        int timeX = x + TEXT_X;
        int timeY = y + 4;
        gfx.drawString(font, time, timeX, timeY, COL_TIME, false);

        // Progress bar
        int barX = timeX;
        int barY = y + 14;
        int barW = Math.max(8, font.width(time));
        double frac = buff.totalTicks() > 0
                ? Math.max(0.0, buff.timeTicks() / (double) buff.totalTicks())
                : 0.0;
        int filled = (int) Math.round(frac * barW);
        int barColour = frac > 0.25 ? COL_BAR_FG : (frac > 0.10 ? COL_BAR_WARN : COL_BAR_CRIT);

        gfx.fill(barX, barY, barX + barW, barY + 2, COL_BAR_BG);
        if (filled > 0) {
            gfx.fill(barX, barY, barX + filled, barY + 2, barColour);
        }

        // Hover tooltip â€” must be in HUD-scaled mouse space
        maybeDrawSlotTooltip(gfx, font, buff, x, y, SLOT_W, SLOT_H, screenW, screenH);
    }

    // -------------------------------------------------------------------------
    // Combo badge
    // -------------------------------------------------------------------------

    private static void drawComboBadge(GuiGraphics gfx, Font font,
                                       int x, int y, ComboTooltipData comboData,
                                       int screenW, int screenH) {
        int bw = 12, bh = 12;
        gfx.fill(x, y, x + bw, y + bh, COL_BG);
        gfx.fill(x, y, x + bw, y + 1, 0xFFAA8800);
        gfx.fill(x, y + bh - 1, x + bw, y + bh, 0xFFAA8800);
        gfx.fill(x, y, x + 1, y + bh, 0xFFAA8800);
        gfx.fill(x + bw - 1, y, x + bw, y + bh, 0xFFAA8800);
        gfx.drawString(font, "\u2605", x + 2, y + 2, 0xFFFFC857, false);

        if (comboData != null && comboData.primary() != null) {
            maybeDrawComboTooltip(gfx, font, x, y, bw, bh, comboData, screenW, screenH);
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
                                              ComboTooltipData comboData,
                                              int screenW, int screenH) {
        int[] mouse = scaledMouse();
        if (mouse[0] < x || mouse[0] > x + w || mouse[1] < y || mouse[1] > y + h) return;

        BuffInstance comboBuff = comboData.primary();
        String title = "\u2605 " + BuffNames.pretty(comboBuff.id());
        String effectLine = formatComboEffects(comboBuff.id(), comboBuff.magnitude());
        String typeLine;
        if (ComboEffectRegistry.isCapstone(comboBuff.id())) {
            typeLine = ComboEffectRegistry.grantsFinalHeart(comboBuff.id())
                    ? "Capstone combo active (+final heart unlock)"
                    : "Capstone combo active";
        } else {
            typeLine = "Pair combo active";
        }
        String reqLine = "Needs: " + ComboEffectRegistry.requiredFoods(comboBuff.id()).stream()
            .map(BuffHudRenderer::friendlyId)
            .sorted()
            .collect(Collectors.joining(" + "));
        String timeStr = formatShortTime(comboBuff.timeTicks()) + " remaining";

        int tw = maxWidth(font, title, effectLine, typeLine, reqLine, timeStr) + 8;
        int th = 52;
        int tx = resolveTooltipX(x, w, tw, screenW);
        int ty = resolveTooltipY(y, th, screenH);

        drawTooltipBox(gfx, tx, ty, tw, th);
        gfx.drawString(font, title, tx + 4, ty + 3, 0xFFFFC857, false);
        gfx.drawString(font, effectLine, tx + 4, ty + 13, 0xFFFFFFFF, false);
        gfx.drawString(font, typeLine, tx + 4, ty + 23, COL_HEART_ON, false);
        gfx.drawString(font, reqLine, tx + 4, ty + 33, 0xFFAAAAAA, false);
        gfx.drawString(font, timeStr, tx + 4, ty + 43, COL_TIME, false);
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
            ResourceLocation id = ResourceLocation.parse(baseSource(source));
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item != null && item != BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
                gfx.pose().pushPose();
                gfx.pose().translate(x, y, 0.0F);
                gfx.pose().scale(0.82F, 0.82F, 1.0F);
                gfx.renderItem(new ItemStack(item), 0, 0);
                gfx.pose().popPose();
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
        if (min > 0) return String.format("%d:%02d", min, rem);
        return rem + "s";
    }

    private static String formatHearts(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) return Integer.toString((int) Math.rint(value));
        return String.format("%.1f", value);
    }

    private static String friendlyId(String source) {
        // Turn "minecraft:cooked_beef" â†’ "Cooked Beef"
        try {
            String path = ResourceLocation.parse(baseSource(source)).getPath();
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

    private static ComboTooltipData buildComboTooltipData(List<BuffInstance> buffs) {
        List<BuffInstance> combos = buffs.stream()
                .filter(BuffHudRenderer::isCombo)
                .sorted(Comparator
                        .comparing((BuffInstance buff) -> !ComboEffectRegistry.isCapstone(buff.id()))
                        .thenComparing((BuffInstance buff) -> -buff.timeTicks()))
                .toList();

        if (combos.isEmpty()) {
            return null;
        }

        return new ComboTooltipData(combos.getFirst(), combos);
    }

    private static String formatComboEffects(String comboId, double comboMagnitude) {
        Map<String, Double> effects = ComboEffectRegistry.effects(comboId);
        if (effects.isEmpty()) {
            return "No combo effects";
        }

        return effects.entrySet().stream()
                .map(entry -> BuffNames.pretty(entry.getKey()) + " " + formatSigned(entry.getValue() * comboMagnitude))
                .collect(Collectors.joining(", "));
    }

    private static String formatSigned(double value) {
        return String.format("%+.2f", value);
    }

    private static String baseSource(String source) {
        int idx = source.indexOf('#');
        return idx >= 0 ? source.substring(0, idx) : source;
    }

    private static List<BuffInstance> groupedFoodSlots(List<BuffInstance> foodBuffs) {
        Map<String, BuffInstance> grouped = new LinkedHashMap<>();
        foodBuffs.stream()
                .sorted((a, b) -> Long.compare(a.created(), b.created()))
                .forEach(buff -> grouped.putIfAbsent(baseSource(buff.source()), buff));
        return List.copyOf(grouped.values());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
