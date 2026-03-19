package com.derko.advancedfoodsystem.events;

import com.derko.advancedfoodsystem.commands.AdvFoodCommand;
import com.derko.advancedfoodsystem.config.ConfigManager;
import com.derko.advancedfoodsystem.config.FoodBuffEntry;
import com.derko.advancedfoodsystem.data.BuffInstance;
import com.derko.advancedfoodsystem.data.BuffMath;
import com.derko.advancedfoodsystem.data.BuffStorage;
import com.derko.advancedfoodsystem.gameplay.BuffNames;
import com.derko.advancedfoodsystem.gameplay.BuffTicker;
import com.derko.seamlessapi.api.BuffData;
import com.derko.seamlessapi.api.BuffEvents;
import com.derko.seamlessapi.api.BuffModifiers;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.Map;

public final class CommonEvents {
    private CommonEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        AdvFoodCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            List<BuffInstance> buffs = BuffStorage.get(player);
            
            // Fire removal events for all buffs cleared by death
            for (BuffInstance buff : buffs) {
                BuffData buffData = new BuffData(
                        buff.id(),
                        buff.timeTicks(),
                        buff.totalTicks(),
                        buff.magnitude(),
                        buff.healthBonusHearts(),
                        buff.source(),
                        buff.created()
                );
                BuffEvents.BuffRemovedEvent event2 = new BuffEvents.BuffRemovedEvent(
                        player, buffData, BuffEvents.BuffRemovedEvent.RemovalReason.DEATH
                );
                NeoForge.EVENT_BUS.post(event2);
            }
            
            event.getEntity().getPersistentData().remove(BuffStorage.ROOT_KEY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        BuffTicker.tick(serverPlayer);
    }

    @SubscribeEvent
    public static void onFoodConsumed(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItem();
        FoodProperties properties = stack.getFoodProperties(player);
        if (properties == null || properties.nutrition() <= 0) {
            return;
        }

        ResourceLocation keyLoc = stack.getItem().builtInRegistryHolder().key().location();
        String key = keyLoc.toString();

        FoodBuffEntry entry = ConfigManager.foodBuffs().get(key);
        if (entry == null || entry.buffs == null || entry.buffs.isEmpty()) {
            return;
        }

        applyConfiguredFoodBuff(player, key, entry);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack stack = event.getItemStack();
        FoodProperties properties = stack.getFoodProperties(player);
        if (properties == null || properties.nutrition() <= 0) {
            return;
        }

        // Vanilla handles normal food usage when the player is not full.
        if (player.canEat(false)) {
            return;
        }

        ResourceLocation keyLoc = stack.getItem().builtInRegistryHolder().key().location();
        String key = keyLoc.toString();
        FoodBuffEntry entry = ConfigManager.foodBuffs().get(key);
        if (entry == null || entry.buffs == null || entry.buffs.isEmpty()) {
            return;
        }

        // Let the item play its normal use animation even at full hunger.
        // The actual buff is still applied on LivingEntityUseItemEvent.Finish.
        player.startUsingItem(event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    private static boolean applyConfiguredFoodBuff(ServerPlayer player, String sourceKey, FoodBuffEntry entry) {
        if (entry.buffs == null || entry.buffs.isEmpty()) {
            return false;
        }

        int durationTicks = (int) (Math.max(15, entry.durationSeconds) * Math.max(0.1D, ConfigManager.modConfig().system.buffDurationMultiplier)) * 20;
        boolean addedAny = false;
        String primaryBuffId = entry.buffs.getFirst();

        // === Pre-application hook ===
        BuffEvents.BuffApplyingEvent applyingEvent = new BuffEvents.BuffApplyingEvent(
            player, sourceKey, primaryBuffId, entry.magnitude, entry.healthBonusHearts
        );
        NeoForge.EVENT_BUS.post(applyingEvent);
        
        if (applyingEvent.isCanceled()) {
            return false;
        }

        // Check API application filters
        if (!BuffModifiers.shouldApplyBuff(player, sourceKey, primaryBuffId)) {
            return false;
        }

        double globalMagnitudeMult = Math.max(0.1D, ConfigManager.modConfig().system.buffMagnitudeMultiplier);
        double modifiedHealth = BuffModifiers.applyHealthModifiers(player, primaryBuffId, applyingEvent.getHealthBonus());

        int index = 0;
        for (String buffId : entry.buffs) {
            if (buffId == null || buffId.isBlank()) {
                continue;
            }

            double perEffectMult = ConfigManager.effectStrengthMultiplier(buffId);
            double modifiedMagnitude = BuffModifiers.applyMagnitudeModifiers(player, buffId, applyingEvent.getMagnitude());
            double effectiveMagnitude = modifiedMagnitude * globalMagnitudeMult * perEffectMult;

            BuffInstance instance = new BuffInstance(
                    buffId,
                    durationTicks,
                    durationTicks,
                    effectiveMagnitude,
                    modifiedHealth,
                    sourceKey + "#p" + index,
                    player.level().getGameTime()
            );

            if (BuffStorage.add(player, instance)) {
                addedAny = true;
                BuffData buffData = new BuffData(
                        buffId,
                        durationTicks,
                        durationTicks,
                        effectiveMagnitude,
                        modifiedHealth,
                        sourceKey,
                        player.level().getGameTime()
                );
                NeoForge.EVENT_BUS.post(new BuffEvents.BuffAppliedEvent(player, buffData));

                if ("saturation_boost".equals(buffId)) {
                    float add = (float) (2.0F * effectiveMagnitude);
                    player.getFoodData().setSaturation(player.getFoodData().getSaturationLevel() + add);
                }
            }
            index++;
        }

        int debuffIndex = 0;
        if (entry.debuffs != null) {
            for (String debuffId : entry.debuffs) {
                if (debuffId == null || debuffId.isBlank()) {
                    continue;
                }
                double effectMult = ConfigManager.effectStrengthMultiplier(debuffId);
                double effectiveDebuffMag = Math.max(0.0D, entry.debuffMagnitude) * effectMult;
                BuffInstance debuff = new BuffInstance(
                        debuffId,
                        durationTicks,
                        durationTicks,
                        effectiveDebuffMag,
                        0.0D,
                        sourceKey + "#d" + debuffIndex,
                        player.level().getGameTime()
                );
                BuffStorage.add(player, debuff);
                debuffIndex++;
            }
        }

        if (addedAny && ConfigManager.modConfig().notifications.showBuffApplied) {
            String msg = "+" + formatHeartLabel(modifiedHealth) + ", " + BuffNames.pretty(primaryBuffId);
            if ("heart_bonus".equals(primaryBuffId)) {
                msg = "+" + formatHeartLabel(modifiedHealth);
            }
            if (entry.debuffs != null && !entry.debuffs.isEmpty()) {
                msg += " (risk)";
            }
            player.displayClientMessage(Component.literal("\u00a76" + msg), true);
        }

        return addedAny;
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation keyLoc = stack.getItem().builtInRegistryHolder().key().location();
        FoodBuffEntry entry = ConfigManager.foodBuffs().get(keyLoc.toString());
        if (entry == null) {
            return;
        }
        double globalMagnitudeMult = Math.max(0.1D, ConfigManager.modConfig().system.buffMagnitudeMultiplier);

        event.getToolTip().add(Component.literal("Bonus Health: +" + formatHeartLabel(entry.healthBonusHearts)).withStyle(ChatFormatting.GOLD));
        if (entry.buffs != null && !entry.buffs.isEmpty()) {
            for (String buff : entry.buffs) {
                if ("heart_bonus".equals(buff)) {
                    continue;
                }
                double perEffectMult = ConfigManager.effectStrengthMultiplier(buff);
                double effectiveMagnitude = entry.magnitude * globalMagnitudeMult * perEffectMult;
                event.getToolTip().add(Component.literal("Buff: " + BuffNames.pretty(buff) + " x" + String.format("%.2f", effectiveMagnitude)).withStyle(ChatFormatting.YELLOW));
            }
        }

        if (entry.debuffs != null && !entry.debuffs.isEmpty()) {
            for (String debuff : entry.debuffs) {
                double perEffectMult = ConfigManager.effectStrengthMultiplier(debuff);
                double effectiveMagnitude = entry.debuffMagnitude * perEffectMult;
                event.getToolTip().add(Component.literal("Debuff: " + BuffNames.pretty(debuff) + " x" + String.format("%.2f", effectiveMagnitude)).withStyle(ChatFormatting.RED));
            }
        }

        event.getToolTip().add(Component.literal("Strength Mod: global x" + String.format("%.2f", globalMagnitudeMult)).withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Component.literal("Duration: " + entry.durationSeconds + "s").withStyle(ChatFormatting.GRAY));
    }

    private static String stripTrailingZero(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format("%.1f", value);
    }

    private static String formatHeartLabel(double hearts) {
        String number = stripTrailingZero(hearts);
        boolean singular = Math.abs(hearts - 1.0D) < 0.0001D;
        return number + (singular ? " heart" : " hearts");
    }

    /** Damage reduction is capped at 75% to prevent near-immortality. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        List<BuffInstance> buffs = BuffStorage.get(player);
        Map<String, Double> totals = BuffMath.aggregateMagnitudes(buffs);

        double reduction = totals.getOrDefault("damage_reduction", 0.0D);
        double frailty = totals.getOrDefault("frailty", 0.0D);
        if (reduction > 0.0D) {
            reduction = Math.min(0.75D, reduction);
        }

        double increased = 1.0D + Math.min(0.25D, Math.max(0.0D, frailty));
        double reduced = 1.0D - reduction;
        event.setNewDamage((float) (event.getNewDamage() * increased * reduced));
    }
}
