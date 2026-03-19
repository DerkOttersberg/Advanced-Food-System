package com.derko.advancedfoodsystem.client;

import com.derko.advancedfoodsystem.data.BuffInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientBuffState {
    private static List<BuffInstance> buffs = new ArrayList<>();

    private ClientBuffState() {
    }

    public static List<BuffInstance> get() {
        return Collections.unmodifiableList(buffs);
    }

    public static void set(List<BuffInstance> value) {
        buffs = new ArrayList<>(value);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        buffs = new ArrayList<>();
    }
}
