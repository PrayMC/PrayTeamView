package me.praymc.prayteamview.state;

import java.util.UUID;

public record TeamViewMarker(
        UUID uniqueId,
        int markerArgb,
        TeamViewPosition previousPosition,
        TeamViewPosition targetPosition,
        String displayName,
        long receivedAtNanos
) {

    public boolean hasServerPosition() {
        return targetPosition != null;
    }

    public TeamViewPosition positionAt(long nowNanos, long interpolationMillis) {
        if (previousPosition == null || targetPosition == null) return null;

        long durationNanos = Math.max(0L, interpolationMillis) * 1_000_000L;
        if (durationNanos == 0L) return targetPosition;

        double progress = Math.max(0.0, Math.min(1.0,
                (double) (nowNanos - receivedAtNanos) / durationNanos
        ));
        return previousPosition.interpolate(targetPosition, progress);
    }
}
