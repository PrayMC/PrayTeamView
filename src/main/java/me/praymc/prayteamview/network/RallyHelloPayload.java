package me.praymc.prayteamview.network;

import me.praymc.prayteamview.PrayTeamViewClient;
import me.praymc.prayteamview.compat.Ids;
import net.minecraft.network.FriendlyByteBuf;

//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

public record RallyHelloPayload(long epoch)
        //? if >=1.20.5
        implements CustomPacketPayload
{
    public static RallyHelloPayload read(FriendlyByteBuf buffer) {
        if (buffer.readUnsignedShort() != PrayTeamViewClient.RALLY_PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported Rally protocol");
        }
        return new RallyHelloPayload(buffer.readLong());
    }

    public static void write(FriendlyByteBuf buffer, RallyHelloPayload payload) {
        buffer.writeShort(PrayTeamViewClient.RALLY_PROTOCOL_VERSION);
        buffer.writeLong(payload.epoch());
    }

    //? if >=1.20.5 {
    public static final Type<RallyHelloPayload> TYPE = new Type<>(Ids.of("rally_hello"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RallyHelloPayload> CODEC = new StreamCodec<>() {
        @Override
        public RallyHelloPayload decode(RegistryFriendlyByteBuf buffer) {
            return read(buffer);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RallyHelloPayload payload) {
            write(buffer, payload);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
