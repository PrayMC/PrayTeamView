package me.praymc.prayteamview.network;

import me.praymc.prayteamview.compat.Ids;
import net.minecraft.network.FriendlyByteBuf;

//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

public record HelloPayload(int protocolVersion, int capabilities)
        //? if >=1.20.5
        implements CustomPacketPayload
{

    public static HelloPayload read(FriendlyByteBuf buffer) {
        return new HelloPayload(buffer.readUnsignedShort(), buffer.readInt());
    }

    public static void write(FriendlyByteBuf buffer, HelloPayload payload) {
        buffer.writeShort(payload.protocolVersion());
        buffer.writeInt(payload.capabilities());
    }

    //? if >=1.20.5 {
    public static final Type<HelloPayload> TYPE = new Type<>(Ids.of("hello"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HelloPayload> CODEC = new StreamCodec<>() {
        @Override
        public HelloPayload decode(RegistryFriendlyByteBuf buffer) {
            return read(buffer);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, HelloPayload payload) {
            write(buffer, payload);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
