package net.exmo.lotm.client.phone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.lotm.phone.MapWandItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Shows the map wand's current block box. */
public final class MapWandOverlay {
    private MapWandOverlay() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        ItemStack stack = minecraft.player.getMainHandItem();
        if (!(stack.getItem() instanceof MapWandItem)) stack = minecraft.player.getOffhandItem();
        if (!(stack.getItem() instanceof MapWandItem)) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getBoolean("hasA") || !tag.getBoolean("hasB")) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        int x1 = Math.min(tag.getInt("ax"), tag.getInt("bx"));
        int y1 = Math.min(tag.getInt("ay"), tag.getInt("by"));
        int z1 = Math.min(tag.getInt("az"), tag.getInt("bz"));
        int x2 = Math.max(tag.getInt("ax"), tag.getInt("bx")) + 1;
        int y2 = Math.max(tag.getInt("ay"), tag.getInt("by")) + 1;
        int z2 = Math.max(tag.getInt("az"), tag.getInt("bz")) + 1;
        AABB box = new AABB(x1, y1, z1, x2, y2, z2);
        VertexConsumer consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(pose, consumer, box, 0.22f, 0.55f, 0.95f, 1f);
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        pose.popPose();
    }
}
