package me.praymc.prayteamview.render;

import me.praymc.prayteamview.state.RallyPoint;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.function.Supplier;

//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}

//? if <1.21.9 {
/*import org.joml.Matrix4f;
import org.joml.Vector3f;
*///?}

public final class RallyHudRenderer {

    private final Supplier<RallyPoint> pointSupplier;

    public RallyHudRenderer(Supplier<RallyPoint> pointSupplier) {
        this.pointSupplier = pointSupplier;
    }

    //? if <1.21.9 {
    /*private Matrix4f projectionMatrix;

    public void captureProjectionMatrix(Matrix4f matrix) {
        projectionMatrix = new Matrix4f(matrix);
    }
    *///?}

    //? if >=1.21 {
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        render(graphics, deltaTracker.getGameTimeDeltaPartialTick(true));
    }
    //?}

    public void render(GuiGraphics graphics, float partialTick) {
        RallyPoint point = pointSupplier.get();
        Minecraft client = Minecraft.getInstance();
        if (point == null || client.player == null) return;
        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 position = client.player.getPosition(partialTick);
        Vec3 target = new Vec3(point.x(), point.y() + 2.6, point.z());
        //? if >=1.21.9 {
        Vec3 relative = target.subtract(camera.position());
        Vector3fc forward = camera.forwardVector();
        //?} else {
        /*Vec3 relative = target.subtract(camera.getPosition());
        Vector3fc forward = camera.getLookVector();
        *///?}
        if (relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z() <= 0.05) return;
        //? if >=1.21.9 {
        Vec3 projected = client.gameRenderer.projectPointToScreen(target);
        //?} else {
        /*if (projectionMatrix == null) return;
        Matrix4f matrix = new Matrix4f(projectionMatrix)
                .rotateX(camera.getXRot() * (float) (Math.PI / 180))
                .rotateY((camera.getYRot() + 180) * (float) (Math.PI / 180));
        Vector3f ndc = matrix.transformProject(relative.toVector3f());
        Vec3 projected = new Vec3(ndc);
        *///?}
        double screenX = (projected.x + 1) * graphics.guiWidth() / 2;
        double screenY = (1 - projected.y) * graphics.guiHeight() / 2;
        if (!Double.isFinite(screenX) || !Double.isFinite(screenY)
                || screenX < 0 || screenX > graphics.guiWidth() || screenY < 35
                || screenY > graphics.guiHeight() - 45) return;
        Component name = Component.literal(point.name());
        long distance = Math.round(position.distanceTo(new Vec3(point.x(), point.y(), point.z())));
        Component detail = Component.literal(distance + "m");
        int width = Math.max(client.font.width(name), client.font.width(detail));
        int x = (int) screenX;
        int y = (int) screenY;
        graphics.fill(x - width / 2 - 3, y - 3, x + width / 2 + 4, y + 21, 0x99000000);
        graphics.drawString(client.font, name, x - client.font.width(name) / 2, y, 0xFFFFFFFF, false);
        graphics.drawString(client.font, detail, x - client.font.width(detail) / 2, y + 10,
                point.colorArgb(), false);
    }

}
