package com.derko.advancedfoodsystem.data;

import com.derko.advancedfoodsystem.config.ConfigManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BuffMath {
    private BuffMath() {
    }

    public static Map<String, Double> aggregateMagnitudes(List<BuffInstance> buffs) {
        Map<String, Double> map = new HashMap<>();
        for (BuffInstance buff : buffs) {
            double multiplier = ConfigManager.effectStrengthMultiplier(buff.id());
            map.merge(buff.id(), buff.magnitude() * multiplier, Double::sum);
        }
        return map;
    }
}
