package com.derko.advancedfoodsystem.network;

import com.derko.advancedfoodsystem.AdvancedFoodSystemMod;
import com.derko.advancedfoodsystem.data.BuffInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record BuffSyncPayload(List<BuffInstance> buffs) implements CustomPacketPayload {
    public static final Type<BuffSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(AdvancedFoodSystemMod.MOD_ID, "buff_sync"));

    public static final StreamCodec<FriendlyByteBuf, BuffSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BuffSyncPayload decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<BuffInstance> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(new BuffInstance(
                        buf.readUtf(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readDouble(),
                    buf.readDouble(),
                        buf.readUtf(),
                        buf.readVarLong()
                ));
            }
            return new BuffSyncPayload(list);
        }

        @Override
        public void encode(FriendlyByteBuf buf, BuffSyncPayload payload) {
            buf.writeVarInt(payload.buffs().size());
            for (BuffInstance buff : payload.buffs()) {
                buf.writeUtf(buff.id());
                buf.writeVarInt(buff.timeTicks());
                buf.writeVarInt(buff.totalTicks());
                buf.writeDouble(buff.magnitude());
                buf.writeDouble(buff.healthBonusHearts());
                buf.writeUtf(buff.source());
                buf.writeVarLong(buff.created());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
