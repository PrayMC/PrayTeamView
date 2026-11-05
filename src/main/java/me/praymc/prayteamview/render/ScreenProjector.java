package me.praymc.prayteamview.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

//? if <1.21.9 {
/*import org.joml.Matrix4f;
import org.joml.Vector3f;
*///?}

// 월드 좌표를 GUI 좌표로 투영한다. 팀원 마커와 Rally 라벨이 같은 계산을 쓴다.
public final class ScreenProjector {

    // 1.21.9 이전에는 projectPointToScreen 이 없고 getFov 가 private 이라 투영 행렬을 렌더 단계에서 받아둔다.
    // 뷰 회전에 camera.rotation() 을 쓰면 안 된다 — 1.20.1 은 180도 요가 빠져 있고 1.21.1 은 들어 있다.
    // 대신 yaw/pitch 로 뷰 행렬을 직접 만든다. 투영 행렬에는 뷰 보빙까지 들어 있어 그대로 곱하면 된다.
    //? if <1.21.9 {
    /*private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewProjection = new Matrix4f();
    private boolean hasProjection;

    public void captureProjectionMatrix(Matrix4f matrix) {
        projectionMatrix.set(matrix);
        hasProjection = true;
    }
    *///?}

    // 카메라 뒤에 있거나 투영할 수 없으면 null 이다. 화면 밖인지는 호출하는 쪽이 판단한다.
    public ScreenPoint project(Minecraft client, GuiGraphics graphics, double x, double y, double z) {
        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 target = new Vec3(x, y, z);

        //? if >=1.21.9 {
        Vec3 relative = target.subtract(camera.position());
        Vector3fc forward = camera.forwardVector();
        //?} else {
        /*Vec3 relative = target.subtract(camera.getPosition());
        Vector3fc forward = camera.getLookVector();
        *///?}

        double depth = relative.x * forward.x() + relative.y * forward.y() + relative.z * forward.z();
        if (depth <= 0.05) return null;

        //? if >=1.21.9 {
        Vec3 projected = client.gameRenderer.projectPointToScreen(target);
        //?} else {
        /*if (!hasProjection) return null;
        // 바닐라 renderLevel 과 같은 순서(Rx(pitch) 다음 Ry(yaw+180))로 뷰 회전을 직접 쌓는다.
        viewProjection.set(projectionMatrix)
                .rotateX(camera.getXRot() * DEG_TO_RAD)
                .rotateY((camera.getYRot() + 180.0F) * DEG_TO_RAD);
        Vector3f ndc = viewProjection.transformProject(relative.toVector3f());
        Vec3 projected = new Vec3(ndc);
        *///?}

        double screenX = (projected.x + 1.0) * graphics.guiWidth() / 2.0;
        double screenY = (1.0 - projected.y) * graphics.guiHeight() / 2.0;
        if (!Double.isFinite(screenX) || !Double.isFinite(screenY)) return null;
        return new ScreenPoint(screenX, screenY);
    }
}
