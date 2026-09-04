package me.praymc.prayteamview;

import me.praymc.prayteamview.compat.Ids;
import me.praymc.prayteamview.config.TeamViewConfig;
import me.praymc.prayteamview.network.HelloPayload;
import me.praymc.prayteamview.network.ResetPayload;
import me.praymc.prayteamview.network.SnapshotPayload;
import me.praymc.prayteamview.render.TeamViewHudRenderer;
import me.praymc.prayteamview.state.TeamViewState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
//?} else {
/*import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
*///?}

//? if >=1.21.6 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
*///?}

public final class PrayTeamViewClient implements ClientModInitializer {

    public static final String MOD_ID = Ids.MOD_ID;
    public static final int PROTOCOL_VERSION = 1;

    private final TeamViewState state = new TeamViewState();

    @Override
    public void onInitializeClient() {
        TeamViewConfig config = TeamViewConfig.load();

        // 1.20.5 미만 경로의 핸들러는 넷티 스레드에서 불린다 — 버퍼를 즉시 읽고 상태 변경만 클라이언트 스레드로 넘긴다
        //? if >=1.20.5 {
        PayloadTypeRegistry.playC2S().register(HelloPayload.TYPE, HelloPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SnapshotPayload.TYPE, SnapshotPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ResetPayload.TYPE, ResetPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(SnapshotPayload.TYPE, (payload, context) ->
                state.apply(
                        payload,
                        PROTOCOL_VERSION,
                        config.remoteInterpolationMillis,
                        config.teleportSnapDistance
                )
        );
        ClientPlayNetworking.registerGlobalReceiver(ResetPayload.TYPE, (payload, context) ->
                state.reset(payload.protocolVersion(), PROTOCOL_VERSION, payload.sequence())
        );
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(Ids.of("snapshot"), (client, handler, buffer, sender) -> {
            SnapshotPayload payload = SnapshotPayload.read(buffer);
            client.execute(() -> state.apply(
                    payload,
                    PROTOCOL_VERSION,
                    config.remoteInterpolationMillis,
                    config.teleportSnapDistance
            ));
        });
        ClientPlayNetworking.registerGlobalReceiver(Ids.of("reset"), (client, handler, buffer, sender) -> {
            ResetPayload payload = ResetPayload.read(buffer);
            client.execute(() -> state.reset(payload.protocolVersion(), PROTOCOL_VERSION, payload.sequence()));
        });
        *///?}

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> state.disconnect());
        // 서버의 채널 광고를 받은 뒤에 보내야 클라이언트 register 가 먼저 나가 서버의 즉답 스냅샷이 버려지지 않는다
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (config.enabled && channels.contains(Ids.of("hello"))) {
                //? if >=1.20.5 {
                ClientPlayNetworking.send(new HelloPayload(PROTOCOL_VERSION, 0));
                //?} else {
                /*FriendlyByteBuf buffer = PacketByteBufs.create();
                HelloPayload.write(buffer, new HelloPayload(PROTOCOL_VERSION, 0));
                ClientPlayNetworking.send(Ids.of("hello"), buffer);
                *///?}
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> state.disconnect());

        TeamViewHudRenderer renderer = new TeamViewHudRenderer(state, config);
        //? if >=1.21.6 {
        HudElementRegistry.addLast(Ids.of("team_view"), renderer::render);
        //?} else {
        /*HudRenderCallback.EVENT.register(renderer::render);
        WorldRenderEvents.END.register(context -> renderer.captureProjectionMatrix(context.projectionMatrix()));
        *///?}
    }
}
