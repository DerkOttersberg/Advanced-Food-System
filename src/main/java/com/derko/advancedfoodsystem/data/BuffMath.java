package com.derko.advancedfoodsystem.data;

import com.derko.advancedfoodsystem.config.ConfigManager;
import com.derko.advancedfoodsystem.gameplay.ComboEffectRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BuffMath {
    private BuffMath() {
    }

    public static Map<String, Double> aggregateMagnitudes(List<BuffInstance> buffs) {
        Map<String, Double> map = new HashMap<>();
        for (BuffInstance buff : buffs) {
            if (buff.id().startsWith("combo_")) {
                Map<String, Double> effects = ComboEffectRegistry.effects(buff.id());
                if (!effects.isEmpty()) {
                    for (Map.Entry<String, Double> effect : effects.entrySet()) {
                        double perEffectMult = ConfigManager.effectStrengthMultiplier(effect.getKey());
                        map.merge(effect.getKey(), effect.getValue() * buff.magnitude() * perEffectMult, Double::sum);
                    }
                    continue;
                }
            }

            double multiplier = ConfigManager.effectStrengthMultiplier(buff.id());
            map.merge(buff.id(), buff.magnitude() * multiplier, Double::sum);
        }
        return map;
    }
}
