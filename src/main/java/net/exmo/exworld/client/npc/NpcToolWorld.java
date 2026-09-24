package net.exmo.exworld.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** World marks for the wand: home, post, places and the active route. */
public final class NpcToolWorld {
    private NpcToolWorld() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || NpcToolView.id.isBlank()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || NpcToolClient.held(minecraft) == null) return;
        String dimension = minecraft.level.dimension().location().toString();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        for (int i = 0; i < NpcToolView.places.size(); i++) {
            CompoundTag place = NpcToolView.places.getCompound(i);
            if (!dimension.equals(place.getString("dim"))) continue;
            float[] color = color(place.getString("role"));
            double radius = Math.max(0.4, place.getDouble("r"));
            AABB box = new AABB(place.getDouble("x") - radius, place.getDouble("y"), place.getDouble("z") - radius,
                    place.getDouble("x") + radius, place.getDouble("y") + 2, place.getDouble("z") + radius);
            LevelRenderer.renderLineBox(pose, lines, box, color[0], color[1], color[2], 0.9f);
        }
        for (int i = 0; i < NpcToolView.routes.size(); i++) {
            CompoundTag route = NpcToolView.routes.getCompound(i);
            if (!route.getBoolean("active")) continue;
            ListTag points = route.getList("points", net.minecraft.nbt.Tag.TAG_COMPOUND);
            CompoundTag previous = null;
            for (int p = 0; p < points.size(); p++) {
                CompoundTag point = points.getCompound(p);
                if (previous != null && dimension.equals(previous.getString("dim")) && dimension.equals(point.getString("dim"))) {
                    AABB span = new AABB(previous.getDouble("x"), previous.getDouble("y") + 0.2, previous.getDouble("z"),
                            point.getDouble("x"), point.getDouble("y") + 0.2, point.getDouble("z")).inflate(0.04);
                    LevelRenderer.renderLineBox(pose, lines, span, 0.94f, 0.73f, 0.18f, 0.8f);
                }
                previous = point;
            }
        }
        if (minecraft.player != null) {
            for (net.exmo.exworld.npc.entity.UrbanNpc npc : minecraft.level.getEntitiesOfClass(net.exmo.exworld.npc.entity.UrbanNpc.class, minecraft.player.getBoundingBox().inflate(48), entity -> NpcToolView.id.equals(entity.documentId()))) {
                LevelRenderer.renderLineBox(pose, lines, npc.getBoundingBox().inflate(0.05), 0.94f, 0.73f, 0.18f, 1f);
            }
        }
         if (minecraft.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                 && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
             int mode = Math.floorMod(NpcToolView.mode, net.exmo.exworld.npc.item.NpcWandItem.MODES.length);
             if (mode >= 1 && mode <= 3) {
                 net.minecraft.core.BlockPos pos = hit.getBlockPos();
                 AABB ghost = new AABB(pos.getX() + 0.2, pos.getY() + 1.02, pos.getZ() + 0.2, pos.getX() + 0.8, pos.getY() + 2.1, pos.getZ() + 0.8);
                 float[] ghostColor = switch (mode) {
                     case 1 -> new float[] { 0.94f, 0.73f, 0.18f };
                     case 3 -> new float[] { 0.45f, 0.27f, 0.90f };
                     default -> new float[] { 0.85f, 0.86f, 0.9f };
                 };
                 LevelRenderer.renderLineBox(pose, lines, ghost, ghostColor[0], ghostColor[1], ghostColor[2], 0.55f);
             }
         }
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        for (int i = 0; i < NpcToolView.places.size(); i++) {
            CompoundTag place = NpcToolView.places.getCompound(i);
            if (!dimension.equals(place.getString("dim"))) continue;
            pose.pushPose();
            pose.translate(place.getDouble("x"), place.getDouble("y") + 2.2, place.getDouble("z"));
            pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(-0.025f, -0.025f, 0.025f);
            net.minecraft.network.chat.Component text = net.minecraft.network.chat.Component.literal(place.getString("id"));
            float tx = -minecraft.font.width(text) / 2f;
            minecraft.font.drawInBatch(text, tx, 0, 0xFFF4F5F7, false, pose.last().pose(), minecraft.renderBuffers().bufferSource(), net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, 0x40000000, 0xF000F0);
            pose.popPose();
        }
        minecraft.renderBuffers().bufferSource().endBatch();
        pose.popPose();
    }

    private static float[] color(String role) {
        return switch (role) {
            case "home" -> new float[] { 0.94f, 0.73f, 0.18f };
            case "post" -> new float[] { 0.45f, 0.27f, 0.90f };
            default -> new float[] { 0.24f, 0.52f, 0.15f };
        };
    }
}
