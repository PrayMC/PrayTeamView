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
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import me.praymc.prayteamview.compat.Ids;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.OptionalDouble;
import java.util.OptionalInt;
//?} else {
/*import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;
*///?}

public final class RallyWorldRenderer implements AutoCloseable {

    private final Supplier<RallyPoint> pointSupplier;

    public RallyWorldRenderer(Supplier<RallyPoint> pointSupplier) {
        this.pointSupplier = pointSupplier;
    }

    public void register() {
        //? if >=1.21.9 {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::render);
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
        matrices.pushPose();
        try {
            // 큰 월드 좌표는 double로 카메라 위치를 뺀 뒤 행렬에 넣어 떨림을 줄인다.
            matrices.translate(point.x() - camera.x, point.y() - camera.y, point.z() - camera.z);
            draw(matrices.last().pose(), point.colorArgb());
        } finally {
            matrices.popPose();
        }
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

    private ByteBufferBuilder allocator;
    private MappableRingBuffer vertices;

    private void draw(Matrix4f matrix, int color) {
        if (allocator == null) allocator = new ByteBufferBuilder(16_384);
        BufferBuilder builder = new BufferBuilder(allocator, VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        RallyGeometry.emit((x, y, z) -> builder.addVertex(matrix, x, y, z).setColor(color));
        try (MeshData mesh = builder.buildOrThrow()) {
            int size = mesh.vertexBuffer().remaining();
            if (vertices == null || vertices.size() < size) {
                if (vertices != null) vertices.close();
                vertices = new MappableRingBuffer(() -> "PrayTeamView Rally vertices",
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, size);
            }
            try (GpuBuffer.MappedView mapped = RenderSystem.getDevice().createCommandEncoder()
                    .mapBuffer(vertices.currentBuffer().slice(0, size), false, true)) {
                MemoryUtil.memCopy(mesh.vertexBuffer(), mapped.data());
            }

            var shapeIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES);
            GpuBuffer indexBuffer = shapeIndices.getBuffer(mesh.drawState().indexCount());
            var transforms = RenderSystem.getDynamicUniforms().writeTransform(
                    RenderSystem.getModelViewMatrix(), new Vector4f(1, 1, 1, 1),
                    new Vector3f(), new Matrix4f());
            var target = Minecraft.getInstance().getMainRenderTarget();
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "PrayTeamView Rally", target.getColorTextureView(), OptionalInt.empty(),
                    target.getDepthTextureView(), OptionalDouble.empty())) {
                pass.setPipeline(PIPELINE);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setVertexBuffer(0, vertices.currentBuffer());
                pass.setIndexBuffer(indexBuffer, shapeIndices.type());
                pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
            }
        } finally {
            if (vertices != null) vertices.rotate();
        }
    }

    @Override
    public void close() {
        if (vertices != null) {
            vertices.close();
            vertices = null;
        }
        if (allocator != null) {
            allocator.close();
            allocator = null;
        }
    }
    //?} else {
    /*private void draw(Matrix4f matrix, int color) {
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        var shader = RenderSystem.getShader();
        float[] shaderColor = RenderSystem.getShaderColor().clone();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            drawLegacyMesh(matrix, color);
        } finally {
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.depthMask(depthWrite);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
        }
    }

    @Override
    public void close() {
    }
    *///?}

    //? if <1.21 {
    /*private void drawLegacyMesh(Matrix4f matrix, int color) {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        RallyGeometry.emit((x, y, z) -> builder.vertex(matrix, x, y, z).color(color).endVertex());
        BufferUploader.drawWithShader(builder.end());
    }
    *///?} elif <1.21.9 {
    /*private void drawLegacyMesh(Matrix4f matrix, int color) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        RallyGeometry.emit((x, y, z) -> builder.addVertex(matrix, x, y, z).setColor(color));
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }
    *///?}
}
