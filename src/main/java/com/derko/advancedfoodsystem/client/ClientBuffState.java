package com.derko.advancedfoodsystem.client;

import com.derko.advancedfoodsystem.data.BuffInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClientBuffState {
    private static List<BuffInstance> buffs = new ArrayList<>();
    private static int suppressHurtCameraTicks = 0;

    private ClientBuffState() {
    }

    public static List<BuffInstance> get() {
        return Collections.unmodifiableList(buffs);
    }

    public static void set(List<BuffInstance> value) {
        if (containsRemovedBuff(buffs, value)) {
            suppressHurtCameraTicks = 12;
        }
        buffs = new ArrayList<>(value);
    }

    public static boolean shouldSuppressHurtCamera() {
        return suppressHurtCameraTicks > 0;
    }

    public static void tickSuppression() {
        if (suppressHurtCameraTicks > 0) {
            suppressHurtCameraTicks--;
        }
    }

    private static boolean containsRemovedBuff(List<BuffInstance> previous, List<BuffInstance> next) {
        if (previous.isEmpty()) {
            return false;
        }

        for (BuffInstance oldBuff : previous) {
            boolean stillPresent = next.stream().anyMatch(newBuff ->
                    newBuff.id().equals(oldBuff.id())
                            && newBuff.source().equals(oldBuff.source())
                            && newBuff.created() == oldBuff.created()
            );
            if (!stillPresent) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        buffs = new ArrayList<>();
        suppressHurtCameraTicks = 0;
    }
}
