package me.praymc.prayteamview.render;

import me.praymc.prayteamview.state.RallyPoint;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.function.Supplier;

//? if >=1.21.9 {
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import me.praymc.prayteamview.compat.Ids;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.OptionalDouble;
import java.util.OptionalInt;
//?} else {
/*import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.GameRenderer;
*///?}

public final class RallyWorldRenderer implements AutoCloseable {

    // 빔이 이 각도(라디안)보다 가늘어 보이지 않게 거리에 맞춰 굵기를 키운다. 1080p 에서 약 2픽셀이다.
    private static final double MIN_BEAM_ANGLE = 0.0025;
    // 렌더 거리를 최소로 줄여도 원거리 클리핑 면은 128블록이다. 빔 꼭대기(+64)까지 그 안에 들어오는 거리.
    private static final double MAX_DRAW_DISTANCE = 48.0;

    private final Supplier<RallyPoint> pointSupplier;
    private final Matrix4f ringPose = new Matrix4f();
    private final Matrix4f beamPose = new Matrix4f();

    public RallyWorldRenderer(Supplier<RallyPoint> pointSupplier) {
        this.pointSupplier = pointSupplier;
    }

    public void register() {
        //? if >=1.21.9 {
        // 반투명 지형까지 그려진 뒤에 그려야 물·유리가 빔을 덮지 않는다.
        WorldRenderEvents.END_MAIN.register(this::render);
        //?} else {
        /*WorldRenderEvents.LAST.register(this::render);
        *///?}
    }

    private void render(WorldRenderContext context) {
        RallyPoint point = pointSupplier.get();
        if (point == null) return;
        //? if >=1.21.9 {
        PoseStack matrices = context.matrices();
        Vec3 camera = context.worldState().cameraRenderState.pos;
        //?} else {
        /*PoseStack matrices = context.matrixStack();
        Vec3 camera = context.camera().getPosition();
        *///?}
        // 큰 월드 좌표는 double로 카메라 위치를 뺀 뒤 행렬에 넣어 떨림을 줄인다.
        double dx = point.x() - camera.x;
        double dy = point.y() - camera.y;
        double dz = point.z() - camera.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double distance = Math.sqrt(horizontal * horizontal + dy * dy);
        // 카메라를 중심으로 위치와 크기를 같은 비율로 줄이면 화면에 보이는 모습은 그대로다.
        // 먼 지점은 이렇게 당겨 그려 원거리 클리핑 면에 잘리지 않게 한다.
        float pull = distance > MAX_DRAW_DISTANCE ? (float) (MAX_DRAW_DISTANCE / distance) : 1.0F;
        float beamScale = (float) Math.max(1.0, horizontal * MIN_BEAM_ANGLE / RallyGeometry.BEAM_WIDTH);
        ringPose.set(matrices.last().pose())
                .translate((float) (dx * pull), (float) (dy * pull), (float) (dz * pull))
                .scale(pull);
        beamPose.set(ringPose).scale(beamScale, 1.0F, beamScale);
        draw(ringPose, beamPose, point.colorArgb());
    }

    //? if >=1.21.9 {
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Ids.of("pipeline/rally_solid"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withoutBlend()
                    .build());
    // 색은 건드리지 않고 가려지지 않은 부분에만 깊이를 남긴다.
    // 뒤에 그려지는 입자·날씨·구름이 빔보다 멀리 있으면 빔을 덮지 못한다.
    private static final RenderPipeline DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Ids.of("pipeline/rally_depth"))
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(true)
                    .withColorWrite(false)
                    .withCull(false)
                    .withoutBlend()
                    .build());
    private static final Vector4fc NO_TINT = new Vector4f(1, 1, 1, 1);
    private static final Vector3fc NO_OFFSET = new Vector3f();
    private static final Matrix4fc NO_TEXTURE_TRANSFORM = new Matrix4f();

    private final Matrix4f modelView = new Matrix4f();
    // 모양이 고정이라 정점은 한 번만 올리고 매 프레임에는 변환 행렬만 넘긴다. 색이 바뀔 때만 다시 올린다.
    private GpuBuffer vertices;
    private int verticesColor;

    private void draw(Matrix4f ring, Matrix4f beam, int color) {
        if (vertices == null || verticesColor != color) upload(color);
        Matrix4f view = RenderSystem.getModelViewMatrix();
        GpuBufferSlice ringTransform = RenderSystem.getDynamicUniforms().writeTransform(
                modelView.set(view).mul(ring), NO_TINT, NO_OFFSET, NO_TEXTURE_TRANSFORM);
        GpuBufferSlice beamTransform = RenderSystem.getDynamicUniforms().writeTransform(
                modelView.set(view).mul(beam), NO_TINT, NO_OFFSET, NO_TEXTURE_TRANSFORM);
        var indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
        GpuBuffer indexBuffer = indices.getBuffer(RallyGeometry.VERTEX_COUNT);
        var target = Minecraft.getInstance().getMainRenderTarget();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "PrayTeamView Rally", target.getColorTextureView(), OptionalInt.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indexBuffer, indices.type());
            drawShape(pass, ringTransform, beamTransform);
            pass.setPipeline(DEPTH_PIPELINE);
            drawShape(pass, ringTransform, beamTransform);
        }
    }

    private static void drawShape(RenderPass pass, GpuBufferSlice ringTransform, GpuBufferSlice beamTransform) {
        pass.setUniform("DynamicTransforms", ringTransform);
        pass.drawIndexed(0, 0, RallyGeometry.RING_VERTEX_COUNT, 1);
        pass.setUniform("DynamicTransforms", beamTransform);
        pass.drawIndexed(0, RallyGeometry.RING_VERTEX_COUNT, RallyGeometry.BEAM_VERTEX_COUNT, 1);
    }

    private void upload(int color) {
        close();
        try (ByteBufferBuilder allocator = new ByteBufferBuilder(
                RallyGeometry.VERTEX_COUNT * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(allocator, VertexFormat.Mode.TRIANGLES,
                    DefaultVertexFormat.POSITION_COLOR);
            RallyGeometry.emit((x, y, z) -> builder.addVertex(x, y, z).setColor(color));
            try (MeshData mesh = builder.buildOrThrow()) {
                vertices = RenderSystem.getDevice().createBuffer(() -> "PrayTeamView Rally vertices",
                        GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                verticesColor = color;
            }
        }
    }

    @Override
    public void close() {
        if (vertices != null) {
            vertices.close();
            vertices = null;
        }
    }
    //?} else {
    /*private void draw(Matrix4f ring, Matrix4f beam, int color) {
        var shader = RenderSystem.getShader();
        float[] shaderColor = RenderSystem.getShaderColor().clone();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            drawLegacyMesh(ring, beam, color);
        } finally {
            // glIsEnabled 같은 GL 상태 조회는 스레드 드라이버에서 렌더링을 멈춰 세우므로 쓰지 않는다.
            // 대신 바닐라가 LAST 직전에 남기는 상태(깊이 테스트·깊이 쓰기·컬링 켜짐, 블렌드 꺼짐)로 되돌린다.
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
        }
    }

    @Override
    public void close() {
    }
    *///?}

    //? if <1.21 {
    /*private void drawLegacyMesh(Matrix4f ring, Matrix4f beam, int color) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        RallyGeometry.emitRing((x, y, z) -> builder.vertex(ring, x, y, z).color(color).endVertex());
        RallyGeometry.emitBeam((x, y, z) -> builder.vertex(beam, x, y, z).color(color).endVertex());
        BufferUploader.drawWithShader(builder.end());
    }
    *///?} elif <1.21.9 {
    /*private void drawLegacyMesh(Matrix4f ring, Matrix4f beam, int color) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        RallyGeometry.emitRing((x, y, z) -> builder.addVertex(ring, x, y, z).setColor(color));
        RallyGeometry.emitBeam((x, y, z) -> builder.addVertex(beam, x, y, z).setColor(color));
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }
    *///?}
}
