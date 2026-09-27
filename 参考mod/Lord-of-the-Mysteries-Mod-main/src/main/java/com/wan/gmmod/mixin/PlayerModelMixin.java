package com.wan.gmmod.mixin;

import com.wan.gmmod.client.PendulumClientState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修改玩家模型：当检测到玩家「正在使用灵摆」时，直接旋转其右臂。
 * <p>
 * 注入 {@link PlayerModel#setupAnim} 的 TAIL（原版姿态计算完成之后），
 * 覆盖右臂旋转值，使其抬起 90° 到胸前，从而与灵摆物品动画同步表现。
 * <p>
 * 之所以注入模型的 {@code setupAnim} 而非 {@code PlayerRenderer} 本身，
 * 是因为手臂姿态在 {@code LivingEntityRenderer#render} 中由模型的
 * {@code setupAnim} 最后写入；在渲染器层设置会被其覆盖，
 * 因此在模型姿态计算末尾覆盖才是可靠且正确的「旋转玩家模型手臂」位置。
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {

    protected PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL")
    )
    private void gmmod$rotateArmForPendulum(T entity, float limbSwing, float limbSwingAmount,
                                            float ageInTicks, float netHeadYaw, float headPitch,
                                            CallbackInfo ci) {
        if (entity instanceof Player player && PendulumClientState.isUsing(player.getUUID())) {
            // 右臂抬起 90° 到胸前（可按需微调）
            this.rightArm.xRot = (float) (-Math.PI / 2.0);
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = 0.4F; // 略微内收到胸口
        }
    }
}
