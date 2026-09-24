package io.redspace.irons_artifice.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.irons_artifice.api.GunBones;
import io.redspace.irons_artifice.client.MuzzleFlashEmitter;
import io.redspace.irons_artifice.data.HandOccupancy;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.animation_adjuster.AnimationAdjuster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GunInHandRenderer extends GeoItemRenderer<GunItem> {
    private static final Set<ItemDisplayContext> HAND_PERSPECTIVES = Set.of(
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND
    );

    private @Nullable GunRenderContext context;

    public GunInHandRenderer(GeoModel<GunItem> model) {
        super(model);
    }

    @Override
    public void preRender(PoseStack poseStack, GunItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        this.context = GunRenderContext.capture(animatable, this.currentItemStack, this.renderPerspective, partialTick, packedLight);
        if (isLeftHand(this.renderPerspective)) {
            poseStack.scale(-1f, 1f, 1f);
            poseStack.last().normal().scale(-1f, -1f, 1f);
        }
        adjustBones(model, this.context);
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void applyRenderLayersForBone(PoseStack poseStack, GunItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        super.applyRenderLayersForBone(poseStack, animatable, bone, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
        GunRenderContext renderContext = this.context;
        if (renderContext == null) {
            return;
        }
        String name = bone.getName();
        if (GunBones.SOCKET_MUZZLE.equals(name) && isHandPerspective(renderContext.perspective) && renderContext.ownerId != null) {
            MuzzleFlashEmitter.tryEmit(renderContext.ownerId, poseStack);
        }
        renderContext.attachments.attachments().forEach((socket, attachmentId) -> {
            if (!socket.equals(name)) {
                return;
            }
            AttachmentRenderableRegistry.get(attachmentId).ifPresent(renderer -> {
                poseStack.pushPose();
                renderer.renderAttached(poseStack, bufferSource, packedLight, partialTick);
                poseStack.popPose();
            });
        });
        if (isFirstPerson(renderContext.perspective)) {
            if (GunBones.RIGHT_ARM.equals(name)) {
                renderFirstPersonHand(poseStack, bufferSource, packedLight, true);
            } else if (GunBones.LEFT_ARM.equals(name) && renderContext.occupancy == HandOccupancy.BOTH) {
                renderFirstPersonHand(poseStack, bufferSource, packedLight, false);
            }
        }
    }

    private void adjustBones(BakedGeoModel model, GunRenderContext renderContext) {
        ItemDisplayContext perspective = renderContext.perspective;
        if (perspective != null && HAND_PERSPECTIVES.contains(perspective)) {
            if (!isFirstPerson(perspective)) {
                model.getBone(GunBones.ROOT).ifPresent(root -> {
                    root.updatePosition(0, 0, 0);
                    root.updateRotation(0, 0, 0);
                });
            }
        } else {
            List<GeoBone> bones = new ArrayList<>();
            collectBones(model.topLevelBones(), bones);
            for (GeoBone bone : bones) {
                if (!bone.getName().contains(GunBones.HAMMER)) {
                    bone.updatePosition(0, 0, 0);
                    bone.updateRotation(0, 0, 0);
                    bone.updateScale(1, 1, 1);
                }
            }
        }
        if (renderContext.adjusters == null) {
            return;
        }
        for (AnimationAdjuster adjuster : renderContext.adjusters) {
            adjuster.adjust(model, renderContext);
        }
    }

    private static void collectBones(List<GeoBone> bones, List<GeoBone> collector) {
        for (GeoBone bone : bones) {
            if (bone == null) {
                continue;
            }
            collector.add(bone);
            collectBones(bone.getChildBones(), collector);
        }
    }

    private static void renderFirstPersonHand(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, boolean rightArm) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        ModelPart arm = rightArm ? renderer.getModel().rightArm : renderer.getModel().leftArm;
        arm.x = 0;
        arm.y = 0;
        arm.z = 0;
        arm.xRot = 0;
        arm.yRot = 0;
        arm.zRot = 0;
        ResourceLocation skin = player.getSkin().texture();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(skin));
        poseStack.pushPose();
        poseStack.scale(-1, -1, 1);
        poseStack.translate(1 / 16f, -10 / 16f, 0);
        arm.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private static boolean isLeftHand(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || perspective == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    private static boolean isHandPerspective(ItemDisplayContext perspective) {
        return perspective != null && HAND_PERSPECTIVES.contains(perspective);
    }

    private static boolean isFirstPerson(ItemDisplayContext perspective) {
        return perspective == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }
}
