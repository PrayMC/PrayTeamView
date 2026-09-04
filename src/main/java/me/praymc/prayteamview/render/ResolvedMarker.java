package me.praymc.prayteamview.render;

import me.praymc.prayteamview.state.TeamViewMarker;

public record ResolvedMarker(TeamViewMarker marker, double x, double y, double z, double distance) {
}
