package com.derko.advancedfoodsystem.data;

import net.minecraft.nbt.CompoundTag;

public class BuffInstance {
    public static final String KEY_ID = "id";
    public static final String KEY_TIME = "time";
    public static final String KEY_MAG = "mag";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_CREATED = "created";
    public static final String KEY_TOTAL = "total";
    public static final String KEY_HEALTH_BONUS = "health_bonus_hearts";

    private final String id;
    private int timeTicks;
    private final int totalTicks;
    private final double magnitude;
    private final double healthBonusHearts;
    private final String source;
    private final long created;

    public BuffInstance(String id, int timeTicks, int totalTicks, double magnitude, double healthBonusHearts, String source, long created) {
        this.id = id;
        this.timeTicks = timeTicks;
        this.totalTicks = totalTicks;
        this.magnitude = magnitude;
        this.healthBonusHearts = healthBonusHearts;
        this.source = source;
        this.created = created;
    }

    public String id() {
        return id;
    }

    public int timeTicks() {
        return timeTicks;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public double magnitude() {
        return magnitude;
    }

    public double healthBonusHearts() {
        return healthBonusHearts;
    }

    public String source() {
        return source;
    }

    public long created() {
        return created;
    }

    public void decrement() {
        this.timeTicks--;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_ID, id);
        tag.putInt(KEY_TIME, timeTicks);
        tag.putInt(KEY_TOTAL, totalTicks);
        tag.putDouble(KEY_MAG, magnitude);
        tag.putDouble(KEY_HEALTH_BONUS, healthBonusHearts);
        tag.putString(KEY_SOURCE, source);
        tag.putLong(KEY_CREATED, created);
        return tag;
    }

    public static BuffInstance fromTag(CompoundTag tag) {
        return new BuffInstance(
                tag.getString(KEY_ID),
                tag.getInt(KEY_TIME),
                Math.max(1, tag.contains(KEY_TOTAL) ? tag.getInt(KEY_TOTAL) : tag.getInt(KEY_TIME)),
                tag.getDouble(KEY_MAG),
                tag.contains(KEY_HEALTH_BONUS) ? tag.getDouble(KEY_HEALTH_BONUS) : 0.0D,
                tag.getString(KEY_SOURCE),
                tag.getLong(KEY_CREATED)
        );
    }
}
