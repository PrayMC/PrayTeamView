package me.praymc.prayteamview.network;

import me.praymc.prayteamview.state.RallyPoint;
import me.praymc.prayteamview.compat.Ids;
import net.minecraft.network.FriendlyByteBuf;

//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

public record RallyStatePayload(long epoch, long sequence, RallyPoint point)
        //? if >=1.20.5
        implements CustomPacketPayload
{
    public static RallyStatePayload read(FriendlyByteBuf buffer) {
        if (buffer.readUnsignedShort() != 2) throw new IllegalArgumentException("Unsupported Rally protocol");
        long epoch = buffer.readLong();
        long sequence = buffer.readLong();
        RallyPoint point = buffer.readBoolean()
                ? new RallyPoint(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                        buffer.readInt(), buffer.readUtf(512))
                : null;
        return new RallyStatePayload(epoch, sequence, point);
    }

    public static void write(FriendlyByteBuf buffer, RallyStatePayload payload) {
        buffer.writeShort(2);
        buffer.writeLong(payload.epoch());
        buffer.writeLong(payload.sequence());
        RallyPoint point = payload.point();
        buffer.writeBoolean(point != null);
        if (point != null) {
            buffer.writeDouble(point.x());
            buffer.writeDouble(point.y());
            buffer.writeDouble(point.z());
            buffer.writeInt(point.colorArgb());
            buffer.writeUtf(point.name(), 512);
        }
    }

    //? if >=1.20.5 {
    public static final Type<RallyStatePayload> TYPE = new Type<>(Ids.of("rally_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RallyStatePayload> CODEC = new StreamCodec<>() {
        @Override
        public RallyStatePayload decode(RegistryFriendlyByteBuf buffer) {
            return read(buffer);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RallyStatePayload payload) {
            write(buffer, payload);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
