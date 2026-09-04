package me.praymc.prayteamview.network;

import me.praymc.prayteamview.compat.Ids;
import net.minecraft.network.FriendlyByteBuf;

//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

public record ResetPayload(int protocolVersion, long sequence)
        //? if >=1.20.5
        implements CustomPacketPayload
{

    public static ResetPayload read(FriendlyByteBuf buffer) {
        return new ResetPayload(buffer.readUnsignedShort(), buffer.readLong());
    }

    public static void write(FriendlyByteBuf buffer, ResetPayload payload) {
        buffer.writeShort(payload.protocolVersion());
        buffer.writeLong(payload.sequence());
    }

    //? if >=1.20.5 {
    public static final Type<ResetPayload> TYPE = new Type<>(Ids.of("reset"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResetPayload> CODEC = new StreamCodec<>() {
        @Override
        public ResetPayload decode(RegistryFriendlyByteBuf buffer) {
            return read(buffer);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ResetPayload payload) {
            write(buffer, payload);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
