package me.praymc.prayteamview.network;

import java.util.UUID;

public record TeamViewMemberPayload(
        UUID uniqueId,
        int markerArgb,
        boolean hasPosition,
        double x,
        double y,
        double z,
        String displayNameJson
) {
}
