package me.praymc.prayteamview.render;

import me.praymc.prayteamview.state.RallyPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
//?}

public final class RallyHudRenderer {

    private final Supplier<RallyPoint> pointSupplier;
    private final ScreenProjector projector;

    public RallyHudRenderer(Supplier<RallyPoint> pointSupplier, ScreenProjector projector) {
        this.pointSupplier = pointSupplier;
        this.projector = projector;
    }

    //? if >=1.21 {
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        render(graphics, deltaTracker.getGameTimeDeltaPartialTick(true));
    }
    //?}

    public void render(GuiGraphics graphics, float partialTick) {
        RallyPoint point = pointSupplier.get();
        Minecraft client = Minecraft.getInstance();
        if (point == null || client.player == null) return;
        ScreenPoint screen = projector.project(client, graphics, point.x(), point.y() + 2.6, point.z());
        if (screen == null || screen.x() < 0 || screen.x() > graphics.guiWidth() || screen.y() < 35
                || screen.y() > graphics.guiHeight() - 45) return;
        Vec3 position = client.player.getPosition(partialTick);
        Component name = Component.literal(point.name());
        long distance = Math.round(position.distanceTo(new Vec3(point.x(), point.y(), point.z())));
        Component detail = Component.literal(distance + "m");
        int width = Math.max(client.font.width(name), client.font.width(detail));
        int x = (int) screen.x();
        int y = (int) screen.y();
        graphics.fill(x - width / 2 - 3, y - 3, x + width / 2 + 4, y + 21, 0x99000000);
        graphics.drawString(client.font, name, x - client.font.width(name) / 2, y, 0xFFFFFFFF, false);
        graphics.drawString(client.font, detail, x - client.font.width(detail) / 2, y + 10,
                point.colorArgb(), false);
    }

}
