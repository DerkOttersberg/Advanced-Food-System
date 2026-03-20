package com.derko.advancedfoodsystem.gameplay;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.util.Map;

public final class AttributeController {
    private static final ResourceLocation MAX_HEALTH_ID      = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "max_health");
    private static final ResourceLocation MOVE_SPEED_ID      = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "move_speed");
    private static final ResourceLocation ATTACK_SPEED_ID    = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "attack_speed");
    private static final ResourceLocation ATTACK_DAMAGE_ID   = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "attack_damage");
    private static final ResourceLocation KNOCKBACK_RES_ID   = ResourceLocation.fromNamespaceAndPath("advancedfoodsystem", "knockback_resistance");

    private AttributeController() {
    }

    public static void applyHealthCap(Player player, double desiredMaxHealth) {
        applyHealthCap(player, desiredMaxHealth, false);
    }

    public static void applyHealthCap(Player player, double desiredMaxHealth, boolean silentHealthLoss) {
        if (player.getHealth() > desiredMaxHealth) {
            player.setHealth((float) desiredMaxHealth);
            if (silentHealthLoss) {
                clearDamageVisuals(player);
            }
        }

        applyAddValue(player, Attributes.MAX_HEALTH, MAX_HEALTH_ID, desiredMaxHealth - 20.0D);
    }

    private static void clearDamageVisuals(Player player) {
        setIntField(player, "hurtTime", 0);
        setIntField(player, "hurtDuration", 0);
        setIntField(player, "invulnerableTime", 0);
        setBooleanField(player, "hurtMarked", false);
    }

    private static void setIntField(Player player, String fieldName, int value) {
        Field field = findField(player.getClass(), fieldName);
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.setInt(player, value);
        } catch (Exception ignored) {
        }
    }

    private static void setBooleanField(Player player, String fieldName, boolean value) {
        Field field = findField(player.getClass(), fieldName);
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.setBoolean(player, value);
        } catch (Exception ignored) {
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public static void applyBuffAttributes(Player player, Map<String, Double> totals) {
        double walk = totals.getOrDefault("walk_speed", 0.0D) - totals.getOrDefault("queasy", 0.0D);
        applyMultiplyBase(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_ID,
            walk);

        double attack = totals.getOrDefault("attack_speed", 0.0D);
        attack -= totals.getOrDefault("fatigue", 0.0D);
        applyMultiplyBase(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, attack);

        double attackDamage = totals.getOrDefault("attack_damage", 0.0D);
        applyAddValue(player, Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_ID, attackDamage);

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
