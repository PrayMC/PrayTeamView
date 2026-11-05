package me.praymc.prayteamview.network;

import me.praymc.prayteamview.PrayTeamViewClient;
import me.praymc.prayteamview.state.RallyPoint;
import me.praymc.prayteamview.compat.Ids;
import net.minecraft.network.FriendlyByteBuf;

//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

// valid 가 false 면 받은 쪽에서 버린다.
public record RallyStatePayload(long epoch, long sequence, RallyPoint point, boolean valid)
        //? if >=1.20.5
        implements CustomPacketPayload
{
    private static final RallyStatePayload INVALID = new RallyStatePayload(0L, 0L, null, false);

    // 디코딩 중 예외가 나면 접속이 끊긴다. 모르는 버전이나 깨진 값은 예외 대신 무시할 패킷으로 돌려준다.
    public static RallyStatePayload read(FriendlyByteBuf buffer) {
        if (buffer.readUnsignedShort() != PrayTeamViewClient.RALLY_PROTOCOL_VERSION) {
            // 남은 바이트가 있으면 패킷이 예상보다 크다는 오류가 나므로 끝까지 건너뛴다.
            buffer.skipBytes(buffer.readableBytes());
            return INVALID;
        }
        long epoch = buffer.readLong();
        long sequence = buffer.readLong();
        if (!buffer.readBoolean()) return new RallyStatePayload(epoch, sequence, null, true);
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        int color = buffer.readInt();
        String name = buffer.readUtf();
        if (!RallyPoint.isValid(x, y, z, name)) return INVALID;
        return new RallyStatePayload(epoch, sequence, new RallyPoint(x, y, z, color, name), true);
    }

    public static void write(FriendlyByteBuf buffer, RallyStatePayload payload) {
        buffer.writeShort(PrayTeamViewClient.RALLY_PROTOCOL_VERSION);
        buffer.writeLong(payload.epoch());
        buffer.writeLong(payload.sequence());
        RallyPoint point = payload.point();
        buffer.writeBoolean(point != null);
        if (point != null) {
            buffer.writeDouble(point.x());
            buffer.writeDouble(point.y());
            buffer.writeDouble(point.z());
            buffer.writeInt(point.colorArgb());
            buffer.writeUtf(point.name(), RallyPoint.MAX_NAME_LENGTH);
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
