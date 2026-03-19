package com.derko.advancedfoodsystem.events;

import com.derko.advancedfoodsystem.commands.AdvFoodCommand;
import com.derko.advancedfoodsystem.config.ConfigManager;
import com.derko.advancedfoodsystem.config.FoodBuffEntry;
import com.derko.advancedfoodsystem.data.BuffInstance;
import com.derko.advancedfoodsystem.data.BuffMath;
import com.derko.advancedfoodsystem.data.BuffStorage;
import com.derko.advancedfoodsystem.gameplay.BuffNames;
import com.derko.advancedfoodsystem.gameplay.BuffTicker;
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

        if (applyConfiguredFoodBuff(player, key, entry)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.setItemInHand(event.getHand(), ItemStack.EMPTY);
                }
            }

            player.swing(event.getHand(), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean applyConfiguredFoodBuff(ServerPlayer player, String sourceKey, FoodBuffEntry entry) {
        String secondaryBuffId = entry.buffs.getFirst();

        double durationMult = Math.max(0.1D, ConfigManager.modConfig().system.buffDurationMultiplier);
        double globalMagnitudeMult = Math.max(0.1D, ConfigManager.modConfig().system.buffMagnitudeMultiplier);
        double perEffectMult = ConfigManager.effectStrengthMultiplier(secondaryBuffId);

        int durationTicks = (int) (Math.max(15, entry.durationSeconds) * durationMult) * 20;
        double effectiveMagnitude = entry.magnitude * globalMagnitudeMult * perEffectMult;

        BuffInstance instance = new BuffInstance(
                secondaryBuffId,
                durationTicks,
                durationTicks,
                effectiveMagnitude,
                entry.healthBonusHearts,
                sourceKey,
                player.level().getGameTime()
        );

        boolean added = BuffStorage.add(player, instance);
        if (added && ConfigManager.modConfig().notifications.showBuffApplied) {
            String msg = "+" + formatHeartLabel(entry.healthBonusHearts) + ", " + BuffNames.pretty(secondaryBuffId);
            player.displayClientMessage(Component.literal("\u00a76" + msg), true);
        }

        if (entry.buffs.contains("saturation_boost")) {
            float add = (float) (2.0F * effectiveMagnitude);
            player.getFoodData().setSaturation(player.getFoodData().getSaturationLevel() + add);
        }

        return added;
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation keyLoc = stack.getItem().builtInRegistryHolder().key().location();
        FoodBuffEntry entry = ConfigManager.foodBuffs().get(keyLoc.toString());
        if (entry == null || entry.buffs == null || entry.buffs.isEmpty()) {
            return;
        }

        String secondary = entry.buffs.getFirst();
        double globalMagnitudeMult = Math.max(0.1D, ConfigManager.modConfig().system.buffMagnitudeMultiplier);
        double perEffectMult = ConfigManager.effectStrengthMultiplier(secondary);
        double effectiveMagnitude = entry.magnitude * globalMagnitudeMult * perEffectMult;

        event.getToolTip().add(Component.literal("Bonus Health: +" + formatHeartLabel(entry.healthBonusHearts)).withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.literal("Buff: " + BuffNames.pretty(secondary) + " x" + String.format("%.2f", effectiveMagnitude)).withStyle(ChatFormatting.YELLOW));
        event.getToolTip().add(Component.literal("Strength Mod: global x" + String.format("%.2f", globalMagnitudeMult)
                + " | effect x" + String.format("%.2f", perEffectMult)).withStyle(ChatFormatting.DARK_GRAY));
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
        if (reduction > 0.0D) {
            reduction = Math.min(0.75D, reduction);
            event.setNewDamage((float) (event.getNewDamage() * (1.0D - reduction)));
        }
    }
}
