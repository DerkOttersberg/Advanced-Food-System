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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
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

        String secondaryBuffId = entry.buffs.getFirst();

        // Apply global multipliers from the NeoForge mod settings config
        double durationMult = Math.max(0.1D, ConfigManager.modConfig().system.buffDurationMultiplier);
        double magnitudeMult = Math.max(0.1D, ConfigManager.modConfig().system.buffMagnitudeMultiplier);

        int durationTicks = (int) (Math.max(15, entry.durationSeconds) * durationMult) * 20;
        double magnitude = entry.magnitude * magnitudeMult;

        BuffInstance instance = new BuffInstance(
                secondaryBuffId,
                durationTicks,
                durationTicks,
                magnitude,
                entry.healthBonusHearts,
                key,
                player.level().getGameTime()
        );
        if (BuffStorage.add(player, instance) && ConfigManager.modConfig().notifications.showBuffApplied) {
            String msg = "+" + stripTrailingZero(entry.healthBonusHearts) + " hearts, " + BuffNames.pretty(secondaryBuffId);
            player.displayClientMessage(Component.literal("\u00a76" + msg), true);
        }

        if (entry.buffs.contains("saturation_boost")) {
            float add = (float) (2.0F * magnitude);
            player.getFoodData().setSaturation(player.getFoodData().getSaturationLevel() + add);
        }
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
        String line = "+" + stripTrailingZero(entry.healthBonusHearts) + " hearts | "
                + BuffNames.pretty(secondary)
                + " x" + String.format("%.2f", entry.magnitude)
                + " | " + entry.durationSeconds + "s";
        event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GOLD));
    }

    private static String stripTrailingZero(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return Integer.toString((int) Math.rint(value));
        }
        return String.format("%.1f", value);
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
            reduction = Math.min(0.75D, reduction);   // hard cap: never more than 75%
            event.setNewDamage((float) (event.getNewDamage() * (1.0D - reduction)));
        }
    }
}
