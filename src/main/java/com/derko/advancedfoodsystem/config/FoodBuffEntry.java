package com.derko.advancedfoodsystem.config;

import java.util.ArrayList;
import java.util.List;

public class FoodBuffEntry {
    public List<String> buffs = new ArrayList<>();
    public List<String> debuffs = new ArrayList<>();
    public List<String> tags = new ArrayList<>();
    public int durationSeconds = 60;
    public double magnitude = 0.1D;
    public double debuffMagnitude = 0.04D;
    public double healthBonusHearts = 0.5D;
}
