package me.praymc.prayteamview.state;

public record RallyPoint(double x, double y, double z, int colorArgb, String name) {

    public RallyPoint {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Non-finite Rally position");
        }
        if (name == null || name.length() > 512) {
            throw new IllegalArgumentException("Invalid Rally name");
        }
        colorArgb |= 0xFF000000;
    }
}
