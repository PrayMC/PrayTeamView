package me.praymc.prayteamview.state;

public record RallyPoint(double x, double y, double z, int colorArgb, String name) {

    public static final int MAX_NAME_LENGTH = 512;

    public RallyPoint {
        if (!isValid(x, y, z, name)) throw new IllegalArgumentException("Invalid Rally point");
        colorArgb |= 0xFF000000;
    }

    public static boolean isValid(double x, double y, double z, String name) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)
                && name != null && name.length() <= MAX_NAME_LENGTH;
    }
}
