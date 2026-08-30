package net.exmo.exworld.mixin.client;

import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.client.ChunkGroupRenderCuller;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies tactical render movement to every entity renderer, including modded non-living render pipelines. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void exworld$hideDownedCombatant(E entity, double x, double y, double z,
                                                                float yaw, float partialTick,
                                                                com.mojang.blaze3d.vertex.PoseStack pose,
                                                                net.minecraft.client.renderer.MultiBufferSource buffers,
                                                                int light, CallbackInfo callback) {
        if (BattleClient.shouldHide(entity) || !ChunkGroupRenderCuller.containsPosition(entity.getX(), entity.getZ())) callback.cancel();
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double exworld$presentedX(double value, Entity entity, double x, double y, double z, float yaw, float partialTick) {
        Vec3 presented = BattleClient.presentationPosition(entity.getUUID(), partialTick);
        return presented == null ? value : value + presented.x - entity.getPosition(partialTick).x;
    }
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double exworld$presentedY(double value, Entity entity, double x, double y, double z, float yaw, float partialTick) {
        Vec3 presented = BattleClient.presentationPosition(entity.getUUID(), partialTick);
        return presented == null ? value : value + presented.y - entity.getPosition(partialTick).y;
    }
    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double exworld$presentedZ(double value, Entity entity, double x, double y, double z, float yaw, float partialTick) {
        Vec3 presented = BattleClient.presentationPosition(entity.getUUID(), partialTick);
        return presented == null ? value : value + presented.z - entity.getPosition(partialTick).z;
    }
}
