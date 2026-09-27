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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GunInHandRenderer extends GeoItemRenderer<GunItem> {
    private static final Set<ItemDisplayContext> HAND_PERSPECTIVES = Set.of(
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND
    );

    private @Nullable GunRenderContext context;
    private @Nullable BakedGeoModel renderingModel;
    private final Map<GeoBone, BoneState> adjustedBoneStates = new IdentityHashMap<>();
    private boolean modelAdjusted;
    public GunInHandRenderer(GeoModel<GunItem> model) {
        super(model);
    }

    @Override
    public long getInstanceId(GunItem animatable) {
        GunRenderOwner.Holder holder = GunRenderOwner.current();
        if (holder != null) {
            return GunItem.clientAnimationId(holder.entityId(), holder.hand());
        }
        ItemDisplayContext perspective = this.renderPerspective;
        var player = Minecraft.getInstance().player;
        if (player != null && isFirstPerson(perspective)) {
            boolean left = perspective == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            boolean mainArmIsLeft = player.getMainArm() == HumanoidArm.LEFT;
            InteractionHand hand = left == mainArmIsLeft ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            return GunItem.clientAnimationId(player.getId(), hand);
        }
        ItemStack stack = this.currentItemStack;
        // GUI and ground must not share a hand controller. Stack ids collide across players.
        return stack == null ? 1L : (Long.MIN_VALUE ^ System.identityHashCode(stack));
    }

    @Override
    public void preRender(PoseStack poseStack, GunItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        restoreAdjustedBoneStates();
        this.context = GunRenderContext.capture(animatable, this.currentItemStack, this.renderPerspective, partialTick, packedLight);
        this.modelAdjusted = false;
        this.renderingModel = model;
        if (isLeftHand(this.renderPerspective)) {
            poseStack.scale(-1f, 1f, 1f);
            poseStack.last().normal().scale(-1f, -1f, 1f);
        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void actuallyRender(PoseStack poseStack, GunItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        restoreAdjustedBoneStates();
        if (!isReRender) {
            resetSharedBones(model);
        }
        this.renderingModel = model;
        this.modelAdjusted = false;
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    public void postRender(PoseStack poseStack, GunItem animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        try {
            super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        } finally {
            restoreAdjustedBoneStates();
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, GunItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (!this.modelAdjusted && this.renderingModel != null && this.context != null) {
            this.modelAdjusted = true;
            applyAdjusters(this.renderingModel);
        }
        float posX = bone.getPosX();
        float posY = bone.getPosY();
        float posZ = bone.getPosZ();
        float rotX = bone.getRotX();
        float rotY = bone.getRotY();
        float rotZ = bone.getRotZ();
        float scaleX = bone.getScaleX();
        float scaleY = bone.getScaleY();
        float scaleZ = bone.getScaleZ();
        applyPerspective(bone);
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        bone.updatePosition(posX, posY, posZ);
        bone.updateRotation(rotX, rotY, rotZ);
        bone.updateScale(scaleX, scaleY, scaleZ);
        bone.resetStateChanges();
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

    private void applyAdjusters(BakedGeoModel model) {
        GunRenderContext renderContext = this.context;
        if (renderContext == null || renderContext.adjusters == null) {
            return;
        }
        captureBoneStates(model.topLevelBones());
        for (AnimationAdjuster adjuster : renderContext.adjusters) {
            adjuster.adjust(model, renderContext);
        }
    }

    private void captureBoneStates(List<GeoBone> bones) {
        for (GeoBone bone : bones) {
            adjustedBoneStates.put(bone, new BoneState(bone));
            captureBoneStates(bone.getChildBones());
        }
    }

    private void restoreAdjustedBoneStates() {
        adjustedBoneStates.forEach((bone, state) -> state.restore(bone));
        adjustedBoneStates.clear();
    }

    private record BoneState(float posX, float posY, float posZ, float rotX, float rotY, float rotZ,
                             float scaleX, float scaleY, float scaleZ) {
        private BoneState(GeoBone bone) {
            this(bone.getPosX(), bone.getPosY(), bone.getPosZ(), bone.getRotX(), bone.getRotY(), bone.getRotZ(),
                    bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
        }

        private void restore(GeoBone bone) {
            bone.updatePosition(posX, posY, posZ);
            bone.updateRotation(rotX, rotY, rotZ);
            bone.updateScale(scaleX, scaleY, scaleZ);
        }
    }

    private static void resetSharedBones(BakedGeoModel model) {
        resetSharedBones(model.topLevelBones());
    }

    private static void resetSharedBones(List<GeoBone> bones) {
        for (GeoBone bone : bones) {
            var initial = bone.getInitialSnapshot();
            if (initial != null) {
                bone.updatePosition(initial.getOffsetX(), initial.getOffsetY(), initial.getOffsetZ());
                bone.updateRotation(initial.getRotX(), initial.getRotY(), initial.getRotZ());
                bone.updateScale(initial.getScaleX(), initial.getScaleY(), initial.getScaleZ());
            }
            bone.resetStateChanges();
            resetSharedBones(bone.getChildBones());
        }
    }

    private void applyPerspective(GeoBone bone) {
        ItemDisplayContext perspective = this.renderPerspective;
        String name = bone.getName();
        if (perspective == null || !HAND_PERSPECTIVES.contains(perspective)) {
            if (!name.contains(GunBones.HAMMER)) {
                bone.updatePosition(0, 0, 0);
                bone.updateRotation(0, 0, 0);
                bone.updateScale(1, 1, 1);
            }
            return;
        }
        if (!isFirstPerson(perspective) && GunBones.ROOT.equals(name)) {
            bone.updatePosition(0, 0, 0);
            bone.updateRotation(0, 0, 0);
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
