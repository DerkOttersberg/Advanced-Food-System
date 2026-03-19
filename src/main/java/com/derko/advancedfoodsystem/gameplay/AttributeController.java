package com.derko.advancedfoodsystem.gameplay;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public final class AttributeController {
    private static final ResourceLocation MAX_HEALTH_ID      = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "max_health");
    private static final ResourceLocation MOVE_SPEED_ID      = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "move_speed");
    private static final ResourceLocation ATTACK_SPEED_ID    = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "attack_speed");
    private static final ResourceLocation KNOCKBACK_RES_ID   = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "knockback_resistance");
    private static final ResourceLocation JUMP_STRENGTH_ID   = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "jump_strength");

    private AttributeController() {
    }

    public static void applyHealthCap(Player player, double desiredMaxHealth) {
        applyAddValue(player, Attributes.MAX_HEALTH, MAX_HEALTH_ID, desiredMaxHealth - 20.0D);

        if (player.getHealth() > desiredMaxHealth) {
            player.setHealth((float) desiredMaxHealth);
        }
    }

    public static void applyBuffAttributes(Player player, Map<String, Double> totals) {
        applyMultiplyBase(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_ID,
                totals.getOrDefault("walk_speed", 0.0D));

        double attack = totals.getOrDefault("attack_speed", 0.0D);
        attack += totals.getOrDefault("warrior_boost", 0.0D) * 0.08D;
        applyMultiplyBase(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, attack);

        // jump_height via JUMP_STRENGTH attribute — applies on both sides so client prediction is correct
        applyMultiplyBase(player, Attributes.JUMP_STRENGTH, JUMP_STRENGTH_ID,
                totals.getOrDefault("jump_height", 0.0D));

        double kb = totals.getOrDefault("knockback_resistance", 0.0D);
        applyAddValue(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RES_ID, Math.min(kb, 0.75D));
    }

    private static void applyAddValue(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            instance.removeModifier(existing);
        }

        if (Math.abs(amount) > 0.0001D) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyMultiplyBase(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            instance.removeModifier(existing);
        }

        if (Math.abs(amount) > 0.0001D) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
