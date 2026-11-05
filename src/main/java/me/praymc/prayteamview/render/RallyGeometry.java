package me.praymc.prayteamview.render;

public final class RallyGeometry {

    public static final float RING_RADIUS = 1.5F;
    public static final float RING_WIDTH = 0.1F;
    public static final float BEAM_WIDTH = 0.12F;
    public static final float BEAM_HEIGHT = 64.0F;
    private static final int SEGMENTS = 64;
    // 정점은 고리 다음 빔 순서로 놓여 있어 두 부분을 따로 그릴 수 있다.
    public static final int RING_VERTEX_COUNT = SEGMENTS * 6;
    public static final int BEAM_VERTEX_COUNT = 36;
    public static final int VERTEX_COUNT = RING_VERTEX_COUNT + BEAM_VERTEX_COUNT;
    private static final float[] VERTICES = build();

    private RallyGeometry() {
    }

    public static void emit(VertexSink sink) {
        emit(sink, 0, VERTEX_COUNT);
    }

    public static void emitRing(VertexSink sink) {
        emit(sink, 0, RING_VERTEX_COUNT);
    }

    public static void emitBeam(VertexSink sink) {
        emit(sink, RING_VERTEX_COUNT, VERTEX_COUNT);
    }

    private static void emit(VertexSink sink, int from, int to) {
        for (int i = from * 3; i < to * 3; i += 3) {
            sink.vertex(VERTICES[i], VERTICES[i + 1], VERTICES[i + 2]);
        }
    }

    private static float[] build() {
        float[] vertices = new float[VERTEX_COUNT * 3];
        int offset = 0;
        float inner = RING_RADIUS - RING_WIDTH;
        for (int i = 0; i < SEGMENTS; i++) {
            double a = Math.PI * 2 * i / SEGMENTS;
            double b = Math.PI * 2 * (i + 1) / SEGMENTS;
            float ax = (float) Math.cos(a), az = (float) Math.sin(a);
            float bx = (float) Math.cos(b), bz = (float) Math.sin(b);
            offset = quad(vertices, offset,
                    ax * inner, 0.025F, az * inner,
                    ax * RING_RADIUS, 0.025F, az * RING_RADIUS,
                    bx * RING_RADIUS, 0.025F, bz * RING_RADIUS,
                    bx * inner, 0.025F, bz * inner);
        }
        float half = BEAM_WIDTH / 2;
        for (int i = 0; i < 4; i++) {
            float ax = i == 0 || i == 3 ? -half : half;
            float az = i < 2 ? -half : half;
            int next = (i + 1) % 4;
            float bx = next == 0 || next == 3 ? -half : half;
            float bz = next < 2 ? -half : half;
            offset = quad(vertices, offset, ax, 0.025F, az, bx, 0.025F, bz,
                    bx, BEAM_HEIGHT, bz, ax, BEAM_HEIGHT, az);
        }
        offset = quad(vertices, offset, -half, BEAM_HEIGHT, -half, half, BEAM_HEIGHT, -half,
                half, BEAM_HEIGHT, half, -half, BEAM_HEIGHT, half);
        quad(vertices, offset, -half, 0.025F, -half, -half, 0.025F, half,
                half, 0.025F, half, half, 0.025F, -half);
        return vertices;
    }

    private static int quad(float[] vertices, int offset,
                            float ax, float ay, float az, float bx, float by, float bz,
                            float cx, float cy, float cz, float dx, float dy, float dz) {
        float[] triangles = {ax, ay, az, bx, by, bz, cx, cy, cz, ax, ay, az, cx, cy, cz, dx, dy, dz};
        System.arraycopy(triangles, 0, vertices, offset, triangles.length);
        return offset + triangles.length;
    }

    @FunctionalInterface
    public interface VertexSink {
        void vertex(float x, float y, float z);
    }
}
