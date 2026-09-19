package net.exmo.exworld.client.ship;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.exmo.exworld.ship.model.ShipBlock;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipOccupancy;
import net.exmo.exworld.ship.model.ShipPart;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared 3D hull preview for the editor and upgrade screens. Cubes are tinted by part colour. */
public final class ShipPreviewRenderer {
    public record Pick(int x, int y, int z, int screenX, int screenY) {}

    private ShipPreviewRenderer() {}

    public static Map<Integer, Pick> render(GuiGraphics graphics, ShipHull hull, List<ShipPart> parts, int selectedPacked,
                                            int left, int top, int width, int height, float yaw, float pitch, float zoom) {
        Map<Integer, Pick> picks = new HashMap<>();
        if (hull.isEmpty()) return picks;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(left + width / 2.0, top + height / 2.0, 200);
        float scale = Math.max(8f, zoom);
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-hull.sizeX() / 2.0, -hull.sizeY() / 2.0, -hull.sizeZ() / 2.0);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = pose.last().pose();
        for (ShipBlock block : hull.blocks()) {
            int color = colorFor(block.packed(), parts, selectedPacked);
            cube(buffer, matrix, block.x(), block.y(), block.z(), color);
            Vector3f center = matrix.transformPosition(block.x() + 0.5f, block.y() + 0.5f, block.z() + 0.5f, new Vector3f());
            picks.put(block.packed(), new Pick(block.x(), block.y(), block.z(), Math.round(center.x), Math.round(center.y)));
        }
        var mesh = buffer.build();
        if (mesh != null) com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(mesh);
        RenderSystem.disableDepthTest();
        pose.popPose();
        return picks;
    }

    public static int pick(Map<Integer, Pick> picks, int mouseX, int mouseY) {
        int best = Integer.MIN_VALUE;
        double bestDist = 14;
        for (var entry : picks.entrySet()) {
            double dx = entry.getValue().screenX() - mouseX;
            double dy = entry.getValue().screenY() - mouseY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < bestDist) { bestDist = dist; best = entry.getKey(); }
        }
        return best;
    }

    private static int colorFor(int packed, List<ShipPart> parts, int selected) {
        if (packed == selected) return 0xFFFFFFFF;
        for (ShipPart part : parts) if (ShipOccupancy.contains(part.occupancy(), packed)) return part.color() | 0xFF000000;
        return 0xFF8A93A0;
    }

    private static void cube(BufferBuilder buffer, Matrix4f matrix, int x, int y, int z, int color) {
        float r = ((color >> 16) & 255) / 255f, g = ((color >> 8) & 255) / 255f, b = (color & 255) / 255f, a = 0.92f;
        float x0 = x, y0 = y, z0 = z, x1 = x + 1, y1 = y + 1, z1 = z + 1;
        quad(buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r * 0.7f, g * 0.7f, b * 0.7f, a);
        quad(buffer, matrix, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, r, g, b, a);
        quad(buffer, matrix, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, r * 0.85f, g * 0.85f, b * 0.85f, a);
        quad(buffer, matrix, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r * 0.85f, g * 0.85f, b * 0.85f, a);
        quad(buffer, matrix, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r * 1.05f, g * 1.05f, b * 1.05f, a);
        quad(buffer, matrix, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, r * 0.55f, g * 0.55f, b * 0.55f, a);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        buffer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
    }
}
