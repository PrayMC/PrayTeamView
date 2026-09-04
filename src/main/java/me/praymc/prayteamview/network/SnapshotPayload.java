package me.praymc.prayteamview.network;

import me.praymc.prayteamview.compat.Ids;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//? if >=1.20.5 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}

public record SnapshotPayload(
        int protocolVersion,
        long sequence,
        long serverTick,
        String worldKey,
        List<TeamViewMemberPayload> members
)
        //? if >=1.20.5
        implements CustomPacketPayload
{

    private static final int FLAG_POSITION = 1;
    private static final int FLAG_DISPLAY_NAME = 1 << 1;
    private static final int MAX_MEMBERS = 30;

    public static SnapshotPayload read(FriendlyByteBuf buffer) {
        int protocolVersion = buffer.readUnsignedShort();
        long sequence = buffer.readLong();
        long serverTick = buffer.readLong();
        String worldKey = buffer.readUtf(128);
        int memberCount = buffer.readVarInt();
        if (memberCount < 0 || memberCount > MAX_MEMBERS) {
            throw new IllegalArgumentException("Invalid Team View member count: " + memberCount);
        }

        List<TeamViewMemberPayload> members = new ArrayList<>(memberCount);
        for (int index = 0; index < memberCount; index++) {
            UUID uniqueId = new UUID(buffer.readLong(), buffer.readLong());
            int markerArgb = buffer.readInt();
            int flags = buffer.readUnsignedByte();
            boolean hasPosition = (flags & FLAG_POSITION) != 0;
            double x = hasPosition ? buffer.readDouble() : 0.0;
            double y = hasPosition ? buffer.readDouble() : 0.0;
            double z = hasPosition ? buffer.readDouble() : 0.0;
            String displayName = (flags & FLAG_DISPLAY_NAME) != 0 ? buffer.readUtf(512) : null;
            members.add(new TeamViewMemberPayload(uniqueId, markerArgb, hasPosition, x, y, z, displayName));
        }

        return new SnapshotPayload(protocolVersion, sequence, serverTick, worldKey, List.copyOf(members));
    }

    public static void write(FriendlyByteBuf buffer, SnapshotPayload payload) {
        buffer.writeShort(payload.protocolVersion());
        buffer.writeLong(payload.sequence());
        buffer.writeLong(payload.serverTick());
        buffer.writeUtf(payload.worldKey(), 128);
        buffer.writeVarInt(payload.members().size());

        for (TeamViewMemberPayload member : payload.members()) {
            buffer.writeLong(member.uniqueId().getMostSignificantBits());
            buffer.writeLong(member.uniqueId().getLeastSignificantBits());
            buffer.writeInt(member.markerArgb());
            int flags = member.hasPosition() ? FLAG_POSITION : 0;
            if (member.displayNameJson() != null) flags |= FLAG_DISPLAY_NAME;
            buffer.writeByte(flags);
            if (member.hasPosition()) {
                buffer.writeDouble(member.x());
                buffer.writeDouble(member.y());
                buffer.writeDouble(member.z());
            }
            if (member.displayNameJson() != null) {
                buffer.writeUtf(member.displayNameJson(), 512);
            }
        }
    }

    //? if >=1.20.5 {
    public static final Type<SnapshotPayload> TYPE = new Type<>(Ids.of("snapshot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SnapshotPayload> CODEC = new StreamCodec<>() {
        @Override
        public SnapshotPayload decode(RegistryFriendlyByteBuf buffer) {
            return read(buffer);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SnapshotPayload payload) {
            write(buffer, payload);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
