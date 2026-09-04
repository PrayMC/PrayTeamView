package me.praymc.prayteamview.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.praymc.prayteamview.network.SnapshotPayload;
import me.praymc.prayteamview.network.TeamViewMemberPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class TeamViewState {

    private long lastSequence = -1L;
    private String worldKey;
    private List<TeamViewMarker> markers = List.of();

    public synchronized void apply(SnapshotPayload payload, int expectedProtocolVersion,
                                   long interpolationMillis, double teleportSnapDistance) {
        if (payload.protocolVersion() != expectedProtocolVersion || payload.sequence() <= lastSequence) return;

        long receivedAt = System.nanoTime();
        boolean sameWorld = payload.worldKey().equals(worldKey);
        Map<UUID, TeamViewMarker> previousMarkers = new LinkedHashMap<>();
        for (TeamViewMarker marker : markers) previousMarkers.put(marker.uniqueId(), marker);

        Map<UUID, TeamViewMarker> uniqueMarkers = new LinkedHashMap<>();
        double snapDistanceSquared = Math.max(0.0, teleportSnapDistance)
                * Math.max(0.0, teleportSnapDistance);
        for (TeamViewMemberPayload member : payload.members()) {
            TeamViewPosition targetPosition = member.hasPosition()
                    ? new TeamViewPosition(member.x(), member.y(), member.z(), payload.serverTick())
                    : null;
            TeamViewPosition previousPosition = targetPosition;
            TeamViewMarker previousMarker = previousMarkers.get(member.uniqueId());

            if (sameWorld && targetPosition != null && previousMarker != null
                    && previousMarker.targetPosition() != null
                    && targetPosition.serverTick() > previousMarker.targetPosition().serverTick()
                    && previousMarker.targetPosition().distanceSquared(targetPosition) < snapDistanceSquared) {
                previousPosition = previousMarker.positionAt(receivedAt, interpolationMillis);
            }

            uniqueMarkers.put(member.uniqueId(), new TeamViewMarker(
                    member.uniqueId(),
                    member.markerArgb(),
                    previousPosition,
                    targetPosition,
                    parseDisplayName(member.displayNameJson()),
                    receivedAt
            ));
        }

        lastSequence = payload.sequence();
        worldKey = payload.worldKey();
        markers = List.copyOf(uniqueMarkers.values());
    }

    public synchronized void reset(int protocolVersion, int expectedProtocolVersion, long sequence) {
        if (protocolVersion != expectedProtocolVersion || sequence <= lastSequence) return;
        lastSequence = sequence;
        worldKey = null;
        markers = List.of();
    }

    public synchronized void disconnect() {
        lastSequence = -1L;
        worldKey = null;
        markers = List.of();
    }

    public synchronized List<TeamViewMarker> current(long ttlMillis) {
        // worldKey 는 서버 문맥 토큰이라 클라이언트 차원키와 형식이 다르다 — 월드 필터링은 서버가 한다
        if (worldKey == null) return List.of();
        long now = System.nanoTime();
        long ttlNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0L, ttlMillis));
        return markers.stream()
                .filter(marker -> now - marker.receivedAtNanos() <= ttlNanos)
                .toList();
    }

    private static String parseDisplayName(String json) {
        if (json == null) return null;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (root.isJsonObject() && root.getAsJsonObject().has("text")) {
                return root.getAsJsonObject().get("text").getAsString();
            }
        } catch (RuntimeException ignored) {
        }
        return null;
    }
}
