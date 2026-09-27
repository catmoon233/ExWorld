package com.wan.gmmod.client.render;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.NightmareEyeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 噩梦邪眼的双形态 GeckoLib 模型。
 * <p>
 * 单个模型类按实体的形态状态（{@code isAttackForm()}）在两套资源之间动态切换：
 * <ul>
 *   <li><b>常态</b>：{@code geo/entity/nightmare_eye_normal.geo.json} +
 *       {@code textures/entity/nightmare_eye_normal.png} +
 *       {@code animations/entity/nightmare_eye_normal.animation.json}；</li>
 *   <li><b>攻击形态</b>：{@code geo/entity/nightmare_eye_attack.geo.json} +
 *       {@code textures/entity/nightmare_eye_attack.png} +
 *       {@code animations/entity/nightmare_eye_attack.animation.json}。</li>
 * </ul>
 * 形态由服务端写入并通过 SynchedEntityData 同步，客户端每帧渲染时据此选择资源，
 * 实现一个实体 / 一个渲染器下的无缝变形。
 */
public class NightmareEyeModel extends GeoModel<NightmareEyeEntity> {
    private static final ResourceLocation MODEL_NORMAL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/nightmare_eye_normal.geo.json");
    private static final ResourceLocation MODEL_ATTACK =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "geo/entity/nightmare_eye_attack.geo.json");
    private static final ResourceLocation TEXTURE_NORMAL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/nightmare_eye_normal.png");
    private static final ResourceLocation TEXTURE_ATTACK =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "textures/entity/nightmare_eye_attack.png");
    private static final ResourceLocation ANIMATION_NORMAL =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/nightmare_eye_normal.animation.json");
    private static final ResourceLocation ANIMATION_ATTACK =
            ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "animations/entity/nightmare_eye_attack.animation.json");

    @Override
    public ResourceLocation getModelResource(NightmareEyeEntity animatable) {
        return animatable.isAttackForm() ? MODEL_ATTACK : MODEL_NORMAL;
    }

    @Override
    public ResourceLocation getTextureResource(NightmareEyeEntity animatable) {
        return animatable.isAttackForm() ? TEXTURE_ATTACK : TEXTURE_NORMAL;
    }

    @Override
    public ResourceLocation getAnimationResource(NightmareEyeEntity animatable) {
        return animatable.isAttackForm() ? ANIMATION_ATTACK : ANIMATION_NORMAL;
    }
}
