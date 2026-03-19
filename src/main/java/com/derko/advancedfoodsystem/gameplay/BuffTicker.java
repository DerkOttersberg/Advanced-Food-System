package com.derko.advancedfoodsystem.gameplay;

import com.derko.advancedfoodsystem.config.ConfigManager;
import com.derko.advancedfoodsystem.config.FoodBuffEntry;
import com.derko.advancedfoodsystem.data.BuffInstance;
import com.derko.advancedfoodsystem.data.BuffMath;
import com.derko.advancedfoodsystem.data.BuffStorage;
import com.derko.advancedfoodsystem.network.NetworkHandler;
import com.derko.seamlessapi.api.BuffData;
import com.derko.seamlessapi.api.BuffEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
            } else {
                // === Fire removal event ===
                BuffData buffData = new BuffData(
                        buff.id(),
                        0,
                        buff.totalTicks(),
                        buff.magnitude(),
                        buff.healthBonusHearts(),
                        buff.source(),
                        buff.created()
                );
                BuffEvents.BuffRemovedEvent removedEvent = new BuffEvents.BuffRemovedEvent(
                        player, buffData, BuffEvents.BuffRemovedEvent.RemovalReason.EXPIRED
                );
                NeoForge.EVENT_BUS.post(removedEvent);

                if (ConfigManager.modConfig().notifications.showBuffExpired) {
                    player.displayClientMessage(Component.literal("\u00a77-" + BuffNames.pretty(buff.id())), true);
                }
            }
        }

        if (player.tickCount % 5 == 0) {
            removeInvalidCombos(keep);
            maybeTriggerCombination(player, keep);
        }

        applyHealthScaling(player, keep);

        Map<String, Double> totals = BuffMath.aggregateMagnitudes(keep);
        applyContinuousEffects(player, totals);
        AttributeController.applyBuffAttributes(player, totals);

        maybeDebugHunger(player, totals);

        BuffStorage.set(player, keep);
        NetworkHandler.syncBuffs(player, keep);
    }

    private static void applyHealthScaling(ServerPlayer player, List<BuffInstance> buffs) {
        double baseHearts = ConfigManager.modConfig().system.maxHearts;
        double maxHearts  = ConfigManager.modConfig().system.maxHeartsWithFood;

        Map<String, Double> sourceHeartMap = new HashMap<>();
        for (BuffInstance buff : buffs) {
            if (!buff.source().startsWith("combo:")) {
                String base = BuffStorage.baseSource(buff.source());
                sourceHeartMap.merge(base, buff.healthBonusHearts(), Math::max);
            }
        }
        double slotBonusHearts = sourceHeartMap.values().stream().mapToDouble(Double::doubleValue).sum();

        boolean wholeHeartScaling = ConfigManager.modConfig().system.wholeHeartHealthScaling;
        double appliedSlotBonusHearts = wholeHeartScaling ? Math.floor(slotBonusHearts) : slotBonusHearts;

        boolean comboActive = buffs.stream().anyMatch(b -> b.source().startsWith("combo:") && ComboEffectRegistry.isCapstone(b.id()));

        double desiredHearts;
        if (comboActive) {
            // Combo grants the last heart — sets the player to the full maximum
            desiredHearts = maxHearts;
        } else {
            // Without combo, food slots can add up to (maxHearts - 1); the final heart is combo-only
            desiredHearts = Math.min(maxHearts - 1.0D, baseHearts + appliedSlotBonusHearts);
        }

        AttributeController.applyHealthCap(player, desiredHearts * 2.0D);
    }

    private static void maybeTriggerCombination(ServerPlayer player, List<BuffInstance> keep) {
        Set<String> activeBaseSources = keep.stream()
                .filter(b -> !isCombo(b))
                .map(b -> BuffStorage.baseSource(b.source()))
                .collect(Collectors.toSet());

        Set<String> activeComboIds = ComboEffectRegistry.activeCombos(activeBaseSources);
        Map<String, FoodBuffEntry> foodMap = ConfigManager.foodBuffs();
        boolean riskActive = activeBaseSources.stream().anyMatch(src -> {
            FoodBuffEntry food = foodMap.get(src);
            return food != null && food.tags != null && food.tags.contains("R");
        });

        for (String comboId : activeComboIds) {
            boolean comboAlreadyActive = keep.stream().anyMatch(b -> b.id().equals(comboId));
            if (comboAlreadyActive) {
                continue;
            }

            double mag = ComboEffectRegistry.isCapstone(comboId) && riskActive ? 0.90D : 1.0D;
            int ticks = 1200 * 20;
            keep.add(new BuffInstance(comboId, ticks, ticks, mag, 0.0D, "combo:" + comboId, player.level().getGameTime()));
        }
    }

    private static void removeInvalidCombos(List<BuffInstance> keep) {
        Set<String> activeBaseSources = keep.stream()
                .filter(b -> !isCombo(b))
                .map(b -> BuffStorage.baseSource(b.source()))
                .collect(Collectors.toSet());

        Set<String> validComboIds = ComboEffectRegistry.activeCombos(activeBaseSources);
        keep.removeIf(buff -> isCombo(buff) && !validComboIds.contains(buff.id()));
    }

    private static boolean isCombo(BuffInstance buff) {
        return buff.source().startsWith("combo:");
    }

    private static void applyContinuousEffects(ServerPlayer player, Map<String, Double> totals) {
        if (player.getFoodData().getFoodLevel() <= 0) {
            totals.merge("walk_speed", -0.10D, Double::sum);
        }

        double hungerEfficiency = totals.getOrDefault("hunger_efficiency", 0.0D);
        double appetiteLeak = totals.getOrDefault("appetite_leak", 0.0D);
        double netHungerEfficiency = hungerEfficiency - appetiteLeak;

        if (netHungerEfficiency > 0.0D && player.tickCount % 20 == 0) {
            float currentSat = player.getFoodData().getSaturationLevel();
            float capSat = player.getFoodData().getFoodLevel();
            float restore = (float) Math.min(1.0D, netHungerEfficiency);
            player.getFoodData().setSaturation(Math.min(capSat, currentSat + restore));
        } else if (netHungerEfficiency < 0.0D && player.tickCount % 20 == 0) {
            float currentSat = player.getFoodData().getSaturationLevel();
            float drain = (float) Math.min(1.0D, Math.abs(netHungerEfficiency));
            player.getFoodData().setSaturation(Math.max(0.0F, currentSat - drain));
        }

        double regen = totals.getOrDefault("regeneration", 0.0D);

        if (regen > 0.0D && player.tickCount % 20 == 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) Math.max(0.1D, regen));
        }
    }

    private static void maybeDebugHunger(ServerPlayer player, Map<String, Double> totals) {
        if (!ConfigManager.isHungerDebugEnabled() || player.tickCount % 20 != 0) {
            return;
        }

        int food = player.getFoodData().getFoodLevel();
        float sat = player.getFoodData().getSaturationLevel();
        double hung = totals.getOrDefault("hunger_efficiency", 0.0D);
        double leak = totals.getOrDefault("appetite_leak", 0.0D);
        double net = hung - leak;

        String msg = String.format("Food:%d/20 Sat:%.2f HUNG:%.2f Leak:%.2f Net:%.2f", food, sat, hung, leak, net);
        player.displayClientMessage(Component.literal("\u00a7b" + msg), true);
    }
}
