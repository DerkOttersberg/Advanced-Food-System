package com.derko.advancedfoodsystem.client;

import com.derko.advancedfoodsystem.config.ConfigManager;
import com.derko.advancedfoodsystem.gameplay.BuffNames;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AfsEffectStrengthScreen extends Screen {
    private static final int FIELD_W = 150;
    private static final int FIELD_H = 20;
    private static final int PAGE_SIZE = 10;

    private final Screen parent;
    private final Map<String, EditBox> multiplierBoxes = new LinkedHashMap<>();
    private int page;

    public AfsEffectStrengthScreen(Screen parent) {
        super(Component.literal("Effect Strength Multipliers"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();
        multiplierBoxes.clear();

        int centerX = this.width / 2;
        int leftX = centerX - 155;
        int rightX = centerX + 5;
        int topY = 40;

        List<String> keys = ConfigManager.effectStrengthKeys();
        int totalPages = Math.max(1, (keys.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        int start = page * PAGE_SIZE;
        int end = Math.min(keys.size(), start + PAGE_SIZE);

        for (int i = start; i < end; i++) {
            int rowIndex = i - start;
            String buffId = keys.get(i);
            int colX = (rowIndex % 2 == 0) ? leftX : rightX;
            int rowY = topY + (rowIndex / 2) * 34;

            addLabel(colX, rowY, BuffNames.pretty(buffId));

            EditBox box = new EditBox(this.font, colX, rowY + 10, FIELD_W, FIELD_H, Component.empty());
            box.setValue(formatDouble(ConfigManager.effectStrengthMultiplier(buffId)));
            addRenderableWidget(box);
            multiplierBoxes.put(buffId, box);
        }

        int navY = this.height - 52;
        int buttonY = this.height - 28;

        addRenderableWidget(Button.builder(Component.literal("Prev"), b -> {
                    if (page > 0) {
                        page--;
                        rebuildWidgets();
                    }
                })
                .bounds(centerX - 154, navY, 74, 20)
                .build());

        Button pageButton = Button.builder(Component.literal("Page " + (page + 1) + "/" + totalPages), b -> {})
                .bounds(centerX - 76, navY, 152, 20)
                .build();
        pageButton.active = false;
        addRenderableWidget(pageButton);

        addRenderableWidget(Button.builder(Component.literal("Next"), b -> {
                    if (page < totalPages - 1) {
                        page++;
                        rebuildWidgets();
                    }
                })
                .bounds(centerX + 80, navY, 74, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> applyAndClose())
                .bounds(centerX - 154, buttonY, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Reset 1.00"), b -> resetToDefault())
                .bounds(centerX - 50, buttonY, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(centerX + 54, buttonY, 100, 20)
                .build());
    }

    private void addLabel(int x, int y, String text) {
        addRenderableOnly((graphics, mouseX, mouseY, partialTick) ->
                graphics.drawString(this.font, text, x, y, 0xFFE0E0E0, false));
    }

    private void applyAndClose() {
        for (Map.Entry<String, EditBox> entry : multiplierBoxes.entrySet()) {
            double fallback = ConfigManager.effectStrengthMultiplier(entry.getKey());
            double value = parseDouble(entry.getValue().getValue(), fallback, 0.0D, 5.0D);
            ConfigManager.setEffectStrengthMultiplier(entry.getKey(), value);
        }
        ConfigManager.saveEffectStrengths();
        this.minecraft.setScreen(parent);
    }

    private void resetToDefault() {
        for (Map.Entry<String, EditBox> entry : multiplierBoxes.entrySet()) {
            ConfigManager.setEffectStrengthMultiplier(entry.getKey(), 1.0D);
            entry.getValue().setValue("1.00");
        }
    }

    private static double parseDouble(String text, double fallback, double min, double max) {
        try {
            double value = Double.parseDouble(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String formatDouble(double value) {
        return String.format("%.2f", value);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, "Set per-effect multipliers (0.00 to 5.00)", this.width / 2, 22, 0xFFB0B0B0);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}