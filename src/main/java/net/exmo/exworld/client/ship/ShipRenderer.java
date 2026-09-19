package net.exmo.exworld.client.ship;

import com.mojang.blaze3d.vertex.PoseStack;
import net.exmo.exworld.ship.assembly.ShipBlockStates;
import net.exmo.exworld.ship.entity.ShipEntity;
import net.exmo.exworld.ship.model.ShipBlock;
import net.exmo.exworld.ship.view.ShipView;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Bakes on revision change conceptually by skipping empty hulls; per-section frustum culling keeps FPS down. */
public final class ShipRenderer extends EntityRenderer<ShipEntity> {
    private final BlockRenderDispatcher dispatcher;
    private final ShipMeshCache cache = new ShipMeshCache();

    public ShipRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.dispatcher = context.getBlockRenderDispatcher();
        this.shadowRadius = 0;
    }

    @Override
    public void render(ShipEntity entity, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.hull().isEmpty()) return;
        cache.ensure(entity, dispatcher);
        pose.pushPose();
        Vec3 camera = entity.position();
        pose.translate(0, 0, 0);
        AABB cameraBox = entity.getBoundingBox();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        ShipView view = new ShipView(entity.hull());
        for (ShipBlock block : entity.hull().blocks()) {
            AABB box = new AABB(block.x(), block.y(), block.z(), block.x() + 1, block.y() + 1, block.z() + 1).move(entity.position());
            if (!box.intersects(cameraBox.inflate(64))) continue;
            BlockState state = ShipBlockStates.parse(block.block());
            if (state.isAir()) continue;
            pose.pushPose();
            pose.translate(block.x(), block.y(), block.z());
            int packedLight = light;
            dispatcher.renderSingleBlock(state, pose, buffers, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
            cursor.set(block.x(), block.y(), block.z());
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
