package me.praymc.prayteamview.state;

public record TeamViewPosition(double x, double y, double z, long serverTick) {

    public TeamViewPosition interpolate(TeamViewPosition target, double progress) {
        return new TeamViewPosition(
                x + (target.x - x) * progress,
                y + (target.y - y) * progress,
                z + (target.z - z) * progress,
                target.serverTick
        );
    }

    public double distanceSquared(TeamViewPosition other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
