package me.praymc.prayteamview;

import me.praymc.prayteamview.compat.Ids;
import me.praymc.prayteamview.config.TeamViewConfig;
import me.praymc.prayteamview.network.HelloPayload;
import me.praymc.prayteamview.network.RallyHelloPayload;
import me.praymc.prayteamview.network.RallyStatePayload;
import me.praymc.prayteamview.network.ResetPayload;
import me.praymc.prayteamview.network.SnapshotPayload;
import me.praymc.prayteamview.render.RallyHudRenderer;
import me.praymc.prayteamview.render.RallyWorldRenderer;
import me.praymc.prayteamview.render.TeamViewHudRenderer;
import me.praymc.prayteamview.state.RallyPoint;
import me.praymc.prayteamview.state.RallyState;
import me.praymc.prayteamview.state.TeamViewState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

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
    private final RallyState rallyState = new RallyState();
    private ClientLevel rallyWorld;
    private boolean rallyHelloSent;
    private TeamViewConfig.RallyPointSettings rallyConfig;

    @Override
    public void onInitializeClient() {
        TeamViewConfig.TeamViewSettings config = TeamViewConfig.load().teamView;

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
        initializeRally();
    }

    private void initializeRally() {
        rallyConfig = TeamViewConfig.load().rallyPoint;
        //? if >=1.20.5 {
        PayloadTypeRegistry.playC2S().register(RallyHelloPayload.TYPE, RallyHelloPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RallyStatePayload.TYPE, RallyStatePayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(RallyStatePayload.TYPE, (payload, context) ->
                acceptRally(Minecraft.getInstance(), payload));
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(Ids.of("rally_state"), (client, handler, buffer, sender) -> {
            RallyStatePayload payload = RallyStatePayload.read(buffer);
            client.execute(() -> acceptRally(client, payload));
        });
        *///?}

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> disconnectRally());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> disconnectRally());
        ClientTickEvents.START_CLIENT_TICK.register(this::updateRallyWorld);
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(Ids.of("rally_hello"))) updateRallyWorld(client);
        });
        C2SPlayChannelEvents.UNREGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(Ids.of("rally_hello"))) disconnectRally();
        });

        RallyWorldRenderer renderer = new RallyWorldRenderer(this::currentRally);
        renderer.register();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
        RallyHudRenderer hud = new RallyHudRenderer(this::currentRally);
        //? if >=1.21.6 {
        HudElementRegistry.addLast(Ids.of("rally"), hud::render);
        //?} else {
        /*HudRenderCallback.EVENT.register(hud::render);
        WorldRenderEvents.END.register(
                context -> hud.captureProjectionMatrix(context.projectionMatrix()));
        *///?}
    }

    private void acceptRally(Minecraft client, RallyStatePayload payload) {
        updateRallyWorld(client);
        rallyState.apply(payload.epoch(), payload.sequence(), payload.point());
    }

    private void updateRallyWorld(Minecraft client) {
        if (rallyWorld != client.level) {
            rallyWorld = client.level;
            rallyState.beginWorld();
            rallyHelloSent = false;
        }
        if (!rallyConfig.enabled || rallyWorld == null || rallyHelloSent
                || !ClientPlayNetworking.canSend(Ids.of("rally_hello"))) return;
        // 월드마다 epoch를 바꿔 이전 월드에서 늦게 도착한 좌표를 거부한다.
        //? if >=1.20.5 {
        ClientPlayNetworking.send(new RallyHelloPayload(rallyState.epoch()));
        //?} else {
        /*FriendlyByteBuf buffer = PacketByteBufs.create();
        RallyHelloPayload.write(buffer, new RallyHelloPayload(rallyState.epoch()));
        ClientPlayNetworking.send(Ids.of("rally_hello"), buffer);
        *///?}
        rallyHelloSent = true;
    }

    private RallyPoint currentRally() {
        Minecraft client = Minecraft.getInstance();
        if (!rallyConfig.enabled || client.level == null || client.level != rallyWorld
                || client.player == null || client.options.hideGui) return null;
        RallyPoint point = rallyState.current();
        if (point == null || client.player.distanceToSqr(point.x(), point.y(), point.z())
                > rallyConfig.maxRenderDistance * rallyConfig.maxRenderDistance) return null;
        return point;
    }

    private void disconnectRally() {
        rallyWorld = null;
        rallyHelloSent = false;
        rallyState.beginWorld();
    }
}
