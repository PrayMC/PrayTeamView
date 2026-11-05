package me.praymc.prayteamview.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import me.praymc.prayteamview.config.TeamViewConfig;
import me.praymc.prayteamview.state.TeamViewMarker;
import me.praymc.prayteamview.state.TeamViewPosition;
import me.praymc.prayteamview.state.TeamViewState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}

public final class TeamViewHudRenderer {

    private static final Component MARKER_ICON = Component.literal("▼");
    private static final double MARKER_HEIGHT_OFFSET = 0.85;

    private final TeamViewState state;
    private final TeamViewConfig.TeamViewSettings config;
    private final ScreenProjector projector;

    public TeamViewHudRenderer(TeamViewState state, TeamViewConfig.TeamViewSettings config,
                               ScreenProjector projector) {
        this.state = state;
        this.config = config;
        this.projector = projector;
    }

    //? if >=1.21 {
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        render(graphics, deltaTracker.getGameTimeDeltaPartialTick(true));
    }
    //?}

    public void render(GuiGraphics graphics, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (!config.enabled || client.player == null || client.level == null || client.options.hideGui) return;

        List<TeamViewMarker> markers = state.current(config.snapshotTtlMillis);
        if (markers.isEmpty()) return;

        Vec3 viewerPosition = client.player.getPosition(partialTick);
        List<ResolvedMarker> resolved = resolve(client, markers, viewerPosition, partialTick, System.nanoTime());
        if (config.screenMarkers) renderScreenMarkers(graphics, client, resolved);
        if (config.directionHud) renderDirectionHud(graphics, client, resolved, viewerPosition);
    }

    private List<ResolvedMarker> resolve(Minecraft client, List<TeamViewMarker> markers,
                                         Vec3 viewerPosition, float partialTick, long nowNanos) {
        List<ResolvedMarker> resolved = new ArrayList<>(markers.size());

        for (TeamViewMarker marker : markers) {
            Player player = client.level.getPlayerByUUID(marker.uniqueId());
            Vec3 position;
            double markerHeight;

            if (player != null) {
                position = player.getPosition(partialTick);
                markerHeight = player.getBbHeight() + MARKER_HEIGHT_OFFSET;
            } else if (marker.hasServerPosition()) {
                TeamViewPosition interpolated = marker.positionAt(nowNanos, config.remoteInterpolationMillis);
                if (interpolated == null) continue;
                position = new Vec3(interpolated.x(), interpolated.y(), interpolated.z());
                markerHeight = EntityType.PLAYER.getHeight() + MARKER_HEIGHT_OFFSET;
            } else {
                continue;
            }

            double distance = viewerPosition.distanceTo(position);
            if (distance <= config.maxRenderDistance) {
                resolved.add(new ResolvedMarker(
                        marker,
                        position.x,
                        position.y + markerHeight,
                        position.z,
                        distance
                ));
            }
        }

        resolved.sort(Comparator.comparingDouble(ResolvedMarker::distance).reversed());
        return resolved;
    }

    private void renderScreenMarkers(GuiGraphics graphics, Minecraft client, List<ResolvedMarker> markers) {
        for (ResolvedMarker resolved : markers) {
            ScreenPoint point = projector.project(client, graphics, resolved.x(), resolved.y(), resolved.z());
            if (point == null || point.x() < -80 || point.x() > graphics.guiWidth() + 80
                    || point.y() < -40 || point.y() > graphics.guiHeight() + 40) continue;

            int color = forceOpaque(resolved.marker().markerArgb());
            drawCenteredString(graphics, client, MARKER_ICON, point.x(), point.y() - 8.0, color);

            if (resolved.distance() >= config.distantDetailsDistance) {
                Component name = Component.literal(displayName(client, resolved));
                Component distance = Component.literal("(" + Math.round(resolved.distance()) + "m)");
                drawCenteredString(graphics, client, name, point.x(), point.y() - 28.0, color);
                drawCenteredString(graphics, client, distance, point.x(), point.y() - 18.0, color);
            }
        }
    }

    private void renderDirectionHud(GuiGraphics graphics, Minecraft client, List<ResolvedMarker> markers,
                                    Vec3 viewerPosition) {
        if (markers.isEmpty()) return;

        int centerX = graphics.guiWidth() / 2;
        int span = Math.min(240, graphics.guiWidth() - 30);
        int y = 8;
        graphics.fill(centerX - span / 2, y + 4, centerX + span / 2, y + 6, 0x66000000);

        //? if >=1.21.9 {
        float cameraYaw = client.gameRenderer.getMainCamera().yRot();
        //?} else {
        /*float cameraYaw = client.gameRenderer.getMainCamera().getYRot();
        *///?}

        for (ResolvedMarker resolved : markers) {
            double dx = resolved.x() - viewerPosition.x;
            double dz = resolved.z() - viewerPosition.z;
            float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float relative = Mth.wrapDegrees(bearing - cameraYaw);
            float x = centerX + relative / 180.0F * (span / 2.0F);
            x = Mth.clamp(x, centerX - span / 2.0F, centerX + span / 2.0F);
            drawHead(graphics, client, resolved, x - 5.0F, y);
        }
    }

    private void drawHead(GuiGraphics graphics, Minecraft client, ResolvedMarker resolved, float x, float y) {
        int color = forceOpaque(resolved.marker().markerArgb());
        pushTranslate(graphics, x, y);
        graphics.fill(-1, -1, 11, 11, 0xCC000000);
        graphics.renderOutline(-1, -1, 12, 12, color);

        PlayerInfo info = client.getConnection() == null
                ? null
                : client.getConnection().getPlayerInfo(resolved.marker().uniqueId());
        if (info == null) {
            String initial = displayName(client, resolved).substring(0, 1).toUpperCase();
            graphics.drawString(client.font, initial, 2, 1, color, true);
            popPose(graphics);
            return;
        }

        //? if >=1.21 {
        PlayerFaceRenderer.draw(graphics, info.getSkin(), 0, 0, 10);
        //?} else {
        /*PlayerFaceRenderer.draw(graphics, info.getSkinLocation(), 0, 0, 10);
        *///?}
        popPose(graphics);
    }

    private void drawCenteredString(GuiGraphics graphics, Minecraft client, Component text,
                                    double centerX, double y, int color) {
        pushTranslate(graphics, (float) centerX, (float) y);
        graphics.drawString(client.font, text, -client.font.width(text) / 2, 0, color, true);
        popPose(graphics);
    }

    private static void pushTranslate(GuiGraphics graphics, float x, float y) {
        //? if >=1.21.6 {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        //?} else {
        /*graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        *///?}
    }

    private static void popPose(GuiGraphics graphics) {
        //? if >=1.21.6 {
        graphics.pose().popMatrix();
        //?} else {
        /*graphics.pose().popPose();
        *///?}
    }

    private String displayName(Minecraft client, ResolvedMarker marker) {
        if (marker.marker().displayName() != null && !marker.marker().displayName().isBlank()) {
            return marker.marker().displayName();
        }
        if (client.level != null) {
            Player player = client.level.getPlayerByUUID(marker.marker().uniqueId());
            if (player != null) return player.getName().getString();
        }
        String id = marker.marker().uniqueId().toString();
        return id.substring(0, 8);
    }

    private static int forceOpaque(int argb) {
        return argb | 0xFF000000;
    }
}
