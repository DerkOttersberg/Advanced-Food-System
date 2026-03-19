package com.derko.advancedfoodsystem.client;

import com.derko.advancedfoodsystem.data.BuffInstance;

import java.util.List;

public record ComboTooltipData(BuffInstance primary, List<BuffInstance> active) {
}
