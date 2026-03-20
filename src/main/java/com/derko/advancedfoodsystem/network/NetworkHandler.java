package com.derko.advancedfoodsystem.network;

import com.derko.advancedfoodsystem.AdvancedFoodSystemMod;
import com.derko.advancedfoodsystem.client.ClientBuffState;
import com.derko.advancedfoodsystem.data.BuffInstance;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(NetworkHandler::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AdvancedFoodSystemMod.MOD_ID).versioned("1");

        registrar.playToClient(
                BuffSyncPayload.TYPE,
                BuffSyncPayload.STREAM_CODEC,
                NetworkHandler::handleBuffSync
        );
    }

    private static void handleBuffSync(BuffSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientBuffState.set(payload.buffs()));
    }

    public static void syncBuffs(ServerPlayer player, List<BuffInstance> buffs) {
        if (player.tickCount % 5 != 0) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new BuffSyncPayload(buffs));
    }

    public static void syncBuffsNow(ServerPlayer player, List<BuffInstance> buffs) {
        PacketDistributor.sendToPlayer(player, new BuffSyncPayload(buffs));
    }
}
