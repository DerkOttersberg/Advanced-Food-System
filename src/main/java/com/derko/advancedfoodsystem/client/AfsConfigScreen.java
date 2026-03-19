package com.derko.advancedfoodsystem.client;

import com.derko.advancedfoodsystem.config.AfsClientConfig;
import com.derko.advancedfoodsystem.config.AfsConfig;
import com.derko.advancedfoodsystem.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class AfsConfigScreen extends Screen {
    private static final int FIELD_W = 150;
    private static final int FIELD_H = 20;
    private static final int ROW_STEP = 34;

    private final Screen parent;

    private EditBox maxHeartsBox;
    private EditBox maxHeartsWithFoodBox;
    private EditBox durationMultBox;
    private EditBox magnitudeMultBox;
    private EditBox hudScaleBox;
    private EditBox hudOffsetXBox;
    private EditBox hudOffsetYBox;

    private boolean wholeHeartScaling;
    private boolean enableHud;
    private String hudPosition;
    private boolean showBuffApplied;
    private boolean showBuffExpired;

    private static final List<String> HUD_POSITIONS = List.of("bottom_right", "bottom_left", "top_right", "top_left");

    public AfsConfigScreen(Screen parent) {
        super(Component.literal("Advanced Food System Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int leftX = centerX - 155;
        int rightX = centerX + 5;
        int rowY = 34;

        wholeHeartScaling = AfsConfig.WHOLE_HEART_HEALTH_SCALING.get();
        enableHud = AfsClientConfig.ENABLE_BUFF_HUD.get();
        hudPosition = AfsClientConfig.HUD_POSITION.get();
        if (!HUD_POSITIONS.contains(hudPosition)) {
            hudPosition = "bottom_right";
        }
        showBuffApplied = AfsClientConfig.SHOW_BUFF_APPLIED.get();
        showBuffExpired = AfsClientConfig.SHOW_BUFF_EXPIRED.get();

        addLabel(leftX, rowY, "Max Hearts");
        maxHeartsBox = createBox(leftX, rowY + 10, Integer.toString(AfsConfig.MAX_HEARTS.get()));

        addLabel(rightX, rowY, "Max Hearts With Food");
        maxHeartsWithFoodBox = createBox(rightX, rowY + 10, Integer.toString(AfsConfig.MAX_HEARTS_WITH_FOOD.get()));
        rowY += ROW_STEP;

        addLabel(leftX, rowY, "Duration Multiplier");
        durationMultBox = createBox(leftX, rowY + 10, formatDouble(AfsConfig.BUFF_DURATION_MULTIPLIER.get()));

        addLabel(rightX, rowY, "Magnitude Multiplier");
        magnitudeMultBox = createBox(rightX, rowY + 10, formatDouble(AfsConfig.BUFF_MAGNITUDE_MULTIPLIER.get()));
        rowY += ROW_STEP;

        addLabel(leftX, rowY, "HUD Scale");
        hudScaleBox = createBox(leftX, rowY + 10, formatDouble(AfsClientConfig.HUD_SCALE.get()));

        addLabel(rightX, rowY, "HUD Position");
        addRenderableWidget(CycleButton.<String>builder(pos -> Component.literal(pos))
                .withValues(HUD_POSITIONS)
                .withInitialValue(hudPosition)
            .create(rightX, rowY + 10, FIELD_W, FIELD_H, Component.literal("HUD Position"), (btn, value) -> hudPosition = value));
        rowY += ROW_STEP;

        addLabel(leftX, rowY, "HUD Offset X");
        hudOffsetXBox = createBox(leftX, rowY + 10, Integer.toString(AfsClientConfig.HUD_OFFSET_X.get()));

        addLabel(rightX, rowY, "HUD Offset Y");
        hudOffsetYBox = createBox(rightX, rowY + 10, Integer.toString(AfsClientConfig.HUD_OFFSET_Y.get()));
        rowY += ROW_STEP;

        addRenderableWidget(CycleButton.onOffBuilder(wholeHeartScaling)
            .create(leftX, rowY, FIELD_W, FIELD_H, Component.literal("Whole-Heart Scaling"), (btn, value) -> wholeHeartScaling = value));
        addRenderableWidget(CycleButton.onOffBuilder(enableHud)
            .create(rightX, rowY, FIELD_W, FIELD_H, Component.literal("Enable Buff HUD"), (btn, value) -> enableHud = value));
        rowY += 24;

        addRenderableWidget(CycleButton.onOffBuilder(showBuffApplied)
            .create(leftX, rowY, FIELD_W, FIELD_H, Component.literal("Show Buff Applied"), (btn, value) -> showBuffApplied = value));
        addRenderableWidget(CycleButton.onOffBuilder(showBuffExpired)
            .create(rightX, rowY, FIELD_W, FIELD_H, Component.literal("Show Buff Expired"), (btn, value) -> showBuffExpired = value));

        int effectButtonY = Math.max(rowY + 28, this.height - 52);
        int actionButtonY = Math.max(effectButtonY + 24, this.height - 28);

        addRenderableWidget(Button.builder(Component.literal("Effect Strengths..."),
                b -> this.minecraft.setScreen(new AfsEffectStrengthScreen(this)))
            .bounds(centerX - 75, effectButtonY, 150, 20)
            .build());

        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> applyAndClose())
            .bounds(centerX - 154, actionButtonY, 150, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parent))
            .bounds(centerX + 4, actionButtonY, 150, 20)
                .build());
    }

    private void addLabel(int x, int y, String text) {
        addRenderableOnly((graphics, mouseX, mouseY, partialTick) ->
                graphics.drawString(this.font, text, x, y, 0xFFE0E0E0, false));
    }

    private EditBox createBox(int x, int y, String value) {
        EditBox box = new EditBox(this.font, x, y, FIELD_W, FIELD_H, Component.empty());
        box.setValue(value);
        addRenderableWidget(box);
        return box;
    }

    private void applyAndClose() {
        AfsConfig.MAX_HEARTS.set(parseInt(maxHeartsBox.getValue(), AfsConfig.MAX_HEARTS.get(), 1, 10));
        AfsConfig.MAX_HEARTS_WITH_FOOD.set(parseInt(maxHeartsWithFoodBox.getValue(), AfsConfig.MAX_HEARTS_WITH_FOOD.get(), 2, 20));
        AfsConfig.WHOLE_HEART_HEALTH_SCALING.set(wholeHeartScaling);
        AfsConfig.BUFF_DURATION_MULTIPLIER.set(parseDouble(durationMultBox.getValue(), AfsConfig.BUFF_DURATION_MULTIPLIER.get(), 0.1D, 10.0D));
        AfsConfig.BUFF_MAGNITUDE_MULTIPLIER.set(parseDouble(magnitudeMultBox.getValue(), AfsConfig.BUFF_MAGNITUDE_MULTIPLIER.get(), 0.1D, 5.0D));

        AfsClientConfig.ENABLE_BUFF_HUD.set(enableHud);
        AfsClientConfig.HUD_POSITION.set(hudPosition);
        AfsClientConfig.HUD_SCALE.set(parseDouble(hudScaleBox.getValue(), AfsClientConfig.HUD_SCALE.get(), 0.5D, 2.0D));
        AfsClientConfig.HUD_OFFSET_X.set(parseInt(hudOffsetXBox.getValue(), AfsClientConfig.HUD_OFFSET_X.get(), -500, 500));
        AfsClientConfig.HUD_OFFSET_Y.set(parseInt(hudOffsetYBox.getValue(), AfsClientConfig.HUD_OFFSET_Y.get(), -500, 500));

        AfsClientConfig.SHOW_BUFF_APPLIED.set(showBuffApplied);
        AfsClientConfig.SHOW_BUFF_EXPIRED.set(showBuffExpired);

        AfsConfig.SPEC.save();
        AfsClientConfig.SPEC.save();
        ConfigManager.refreshFromNeoForgeConfigs();

        this.minecraft.setScreen(parent);
    }

    private static int parseInt(String text, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (Exception ignored) {
            return fallback;
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
        guiGraphics.drawCenteredString(this.font, "General settings and HUD options", this.width / 2, 22, 0xFFB0B0B0);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
