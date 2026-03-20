package com.derko.advancedfoodsystem.events;

import com.derko.advancedfoodsystem.client.ClientBuffState;
import com.derko.advancedfoodsystem.data.BuffInstance;
import com.derko.advancedfoodsystem.data.BuffMath;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Map;

/**
 * Handles events that must run on the logical CLIENT only.
 * Registered conditionally in AdvancedFoodSystemMod.
 */
public final class ClientEvents {

    private ClientEvents() {
    }

    /**
     * PlayerEvent.BreakSpeed fires on the logical client (it drives the break-progress
     * animation and the client-side timing check). Using ClientBuffState here avoids
     * the ServerPlayer cast that previously broke the feature.
     */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        List<BuffInstance> buffs = ClientBuffState.get();
        if (buffs.isEmpty()) {
            return;
        }

        Map<String, Double> totals = BuffMath.aggregateMagnitudes(buffs);

        double mining = totals.getOrDefault("mining_speed", 0.0D);

        if (mining > 0.0D) {
            event.setNewSpeed((float) (event.getOriginalSpeed() * (1.0D + mining)));
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ClientBuffState.tickSuppression();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientBuffState.shouldSuppressHurtCamera()) {
            return;
        }

        // Keep normal camera direction but suppress hurt wobble from silent health cap changes.
        event.setYaw(mc.player.getViewYRot((float) event.getPartialTick()));
        event.setPitch(mc.player.getViewXRot((float) event.getPartialTick()));
        event.setRoll(0.0F);

        mc.player.hurtTime = 0;
        mc.player.hurtDuration = 0;
    }
}
