package com.derko.advancedfoodsystem.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BuffMath {
    private BuffMath() {
    }

    public static Map<String, Double> aggregateMagnitudes(List<BuffInstance> buffs) {
        Map<String, Double> map = new HashMap<>();
        for (BuffInstance buff : buffs) {
            map.merge(buff.id(), buff.magnitude(), Double::sum);
        }
        return map;
    }
}
