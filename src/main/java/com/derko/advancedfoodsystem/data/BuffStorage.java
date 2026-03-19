package com.derko.advancedfoodsystem.data;

import com.derko.advancedfoodsystem.config.ConfigManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BuffStorage {
    public static final String ROOT_KEY = "active_buffs";

    private BuffStorage() {
    }

    public static List<BuffInstance> get(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        ListTag list = persistent.getList(ROOT_KEY, Tag.TAG_COMPOUND);
        List<BuffInstance> result = new ArrayList<>();
        for (Tag tag : list) {
            if (tag instanceof CompoundTag compound) {
                BuffInstance instance = BuffInstance.fromTag(compound);
                if (!instance.id().isBlank() && instance.timeTicks() > 0) {
                    result.add(instance);
                }
            }
        }
        result.sort(Comparator.comparingLong(BuffInstance::created));
        return result;
    }

    public static void set(ServerPlayer player, List<BuffInstance> buffs) {
        ListTag list = new ListTag();
        for (BuffInstance buff : buffs) {
            list.add(buff.toTag());
        }
        player.getPersistentData().put(ROOT_KEY, list);
    }

    public static boolean add(ServerPlayer player, BuffInstance newBuff) {
        List<BuffInstance> current = get(player);
        boolean hasSameFoodSource = current.stream().anyMatch(b -> !isCombo(b) && b.source().equals(newBuff.source()));
        if (hasSameFoodSource) {
            return false;
        }

        int maxActive = Math.min(3, ConfigManager.modConfig().system.maxActiveBuffs);
        long activeFoodSlots = current.stream().filter(b -> !isCombo(b)).count();
        if (activeFoodSlots >= maxActive) {
            return false;
        }

        current.add(newBuff);
        current.sort(Comparator.comparingLong(BuffInstance::created));
        set(player, current);
        return true;
    }

    private static boolean isCombo(BuffInstance buff) {
        return buff.source().startsWith("combo:");
    }
}
