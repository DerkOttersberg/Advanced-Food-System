package com.derko.advancedfoodsystem.gameplay;

import com.derko.advancedfoodsystem.config.ComboEntry;
import com.derko.advancedfoodsystem.config.ConfigManager;
import com.derko.advancedfoodsystem.data.BuffInstance;
import com.derko.advancedfoodsystem.data.BuffMath;
import com.derko.advancedfoodsystem.data.BuffStorage;
import com.derko.advancedfoodsystem.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class BuffTicker {
    private BuffTicker() {
    }

    public static void tick(ServerPlayer player) {
        List<BuffInstance> buffs = new ArrayList<>(BuffStorage.get(player));
        if (buffs.isEmpty()) {
            applyHealthScaling(player, List.of());
            AttributeController.applyBuffAttributes(player, Map.of());
            NetworkHandler.syncBuffs(player, List.of());
            return;
        }

        List<BuffInstance> keep = new ArrayList<>();
        for (BuffInstance buff : buffs) {
            buff.decrement();
            if (buff.timeTicks() > 0) {
                keep.add(buff);
            } else if (ConfigManager.modConfig().notifications.showBuffExpired) {
                player.displayClientMessage(Component.literal("\u00a77-" + BuffNames.pretty(buff.id())), true);
            }
        }

        removeInvalidCombos(keep);

        if (player.tickCount % 5 == 0) {
            maybeTriggerCombination(player, keep);
        }

        applyHealthScaling(player, keep);

        Map<String, Double> totals = BuffMath.aggregateMagnitudes(keep);
        applyContinuousEffects(player, totals);
        AttributeController.applyBuffAttributes(player, totals);

        BuffStorage.set(player, keep);
        NetworkHandler.syncBuffs(player, keep);
    }

    private static void applyHealthScaling(ServerPlayer player, List<BuffInstance> buffs) {
        double baseHearts = ConfigManager.modConfig().system.maxHearts;
        double maxHearts  = ConfigManager.modConfig().system.maxHeartsWithFood;

        double slotBonusHearts = buffs.stream()
                .filter(b -> !b.source().startsWith("combo:"))
                .mapToDouble(BuffInstance::healthBonusHearts)
                .sum();

        boolean comboActive = buffs.stream().anyMatch(b -> b.source().startsWith("combo:"));

        double desiredHearts;
        if (comboActive) {
            // Combo grants the last heart — sets the player to the full maximum
            desiredHearts = maxHearts;
        } else {
            // Without combo, food slots can add up to (maxHearts - 1); the final heart is combo-only
            desiredHearts = Math.min(maxHearts - 1.0D, baseHearts + slotBonusHearts);
        }

        AttributeController.applyHealthCap(player, desiredHearts * 2.0D);
    }

    private static void maybeTriggerCombination(ServerPlayer player, List<BuffInstance> keep) {
        Set<String> activeIds = keep.stream().filter(b -> !isCombo(b)).map(BuffInstance::id).collect(Collectors.toSet());

        for (Map.Entry<String, ComboEntry> combo : ConfigManager.comboBuffs().entrySet()) {
            ComboEntry entry = combo.getValue();
            if (entry.requires.isEmpty() || entry.buffId.isBlank()) {
                continue;
            }

            boolean match = entry.requires.stream().allMatch(activeIds::contains);
            if (!match) {
                continue;
            }

            boolean comboAlreadyActive = keep.stream().anyMatch(b -> b.id().equals(entry.buffId));
            if (comboAlreadyActive) {
                return;
            }

            int ticks = entry.durationSeconds * 20;
            keep.add(new BuffInstance(entry.buffId, ticks, ticks, entry.magnitude, 0.0D, "combo:" + combo.getKey(), player.level().getGameTime()));
            return;
        }
    }

    private static void removeInvalidCombos(List<BuffInstance> keep) {
        Set<String> activeFoodBuffs = keep.stream().filter(b -> !isCombo(b)).map(BuffInstance::id).collect(Collectors.toSet());

        keep.removeIf(buff -> {
            if (!isCombo(buff)) {
                return false;
            }

            String comboKey = buff.source().substring("combo:".length());
            ComboEntry combo = ConfigManager.comboBuffs().get(comboKey);
            if (combo == null || combo.requires == null || combo.requires.isEmpty()) {
                return true;
            }

            return !combo.requires.stream().allMatch(activeFoodBuffs::contains);
        });
    }

    private static boolean isCombo(BuffInstance buff) {
        return buff.source().startsWith("combo:");
    }

    private static void applyContinuousEffects(ServerPlayer player, Map<String, Double> totals) {
        if (player.getFoodData().getFoodLevel() <= 0) {
            totals.merge("walk_speed", -0.10D, Double::sum);
        }

        double regen = totals.getOrDefault("regeneration", 0.0D);

        if (regen > 0.0D && player.tickCount % 20 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) Math.max(0.1D, regen));
        }
    }
}
