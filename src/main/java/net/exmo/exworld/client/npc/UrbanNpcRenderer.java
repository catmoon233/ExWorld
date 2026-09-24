package net.exmo.exworld.client.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.exmo.exworld.npc.entity.UrbanNpc;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Wide or slim player model. Remote and named skins resolve on the client and fall back to Steve. */
public final class UrbanNpcRenderer extends MobRenderer<UrbanNpc, PlayerModel<UrbanNpc>> {
    private final PlayerModel<UrbanNpc> wide;
    private final PlayerModel<UrbanNpc> slim;

    public UrbanNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.wide = this.model;
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        ModelManager models = context.getModelManager();
        addLayer(new MatchingArmor(this, wide,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), models));
        addLayer(new MatchingArmor(this, slim,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)), models));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(UrbanNpc entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        this.model = entity.slim() ? slim : wide;
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(UrbanNpc entity) {
        return NpcRemoteSkins.resolve(entity.textureId(), entity.skinKind());
    }

    @Override
    protected void renderNameTag(UrbanNpc entity, Component name, PoseStack pose, MultiBufferSource buffer, int light, float partialTick) {
        super.renderNameTag(entity, name, pose, buffer, light, partialTick);
        if (entity.speech() == null || entity.speech().isBlank()) return;
        pose.pushPose();
        pose.translate(0, 0.28, 0);
        super.renderNameTag(entity, Component.literal(entity.speech()), pose, buffer, light, partialTick);
        pose.popPose();
    }

    private static final class MatchingArmor extends HumanoidArmorLayer<UrbanNpc, PlayerModel<UrbanNpc>, HumanoidModel<UrbanNpc>> {
        private final UrbanNpcRenderer owner;
        private final PlayerModel<UrbanNpc> match;

        private MatchingArmor(UrbanNpcRenderer owner, PlayerModel<UrbanNpc> match, HumanoidModel<UrbanNpc> inner, HumanoidModel<UrbanNpc> outer, ModelManager models) {
            super(owner, inner, outer, models);
            this.owner = owner;
            this.match = match;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, UrbanNpc entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (owner.getModel() != match) return;
            super.render(pose, buffer, light, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        }
    }
}
