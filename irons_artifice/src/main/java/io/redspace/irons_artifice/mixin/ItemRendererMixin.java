package io.redspace.irons_artifice.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.irons_artifice.client.gun.GunRenderOwner;
import io.redspace.irons_artifice.item.GunItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("HEAD")
    )
    private void irons_artifice$pushGunOwner(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext context,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            Level level,
            int light,
            int overlay,
            int seed,
            CallbackInfo ci
    ) {
        if (stack.getItem() instanceof GunItem) {
            GunRenderOwner.push(entity, leftHand);
        }
    }

    @Inject(
            method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
            at = @At("RETURN")
    )
    private void irons_artifice$popGunOwner(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext context,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            Level level,
            int light,
            int overlay,
            int seed,
            CallbackInfo ci
    ) {
        if (stack.getItem() instanceof GunItem) {
            GunRenderOwner.pop();
        }
    }
}
