package net.exmo.exworld.mixin.client;

import net.exmo.exworld.client.perspective.DungeonPerspective;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import net.exmo.exworld.client.battle.BattleClient;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Applies the dungeon rotation before vanilla computes the detached-camera position and wall collision.
 * A late viewport-angle event changes only rendering orientation and leaves a player-yaw feedback loop.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @ModifyArgs(method = "setup", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0))
    private void exworld$fixDungeonCameraBeforePositioning(Args args) {
        if (!DungeonPerspective.active()) return;
        args.set(0, DungeonPerspective.cameraYaw());
        args.set(1, DungeonPerspective.cameraPitch());
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void exworld$anchorBattleCamera(net.minecraft.world.level.BlockGetter level, net.minecraft.world.entity.Entity entity,
                                            boolean detached, boolean mirrored, float partialTick, CallbackInfo callback) {
        if (net.exmo.exworld.client.camera.AdvancedCameraDirector.active()) {
            var director = net.exmo.exworld.client.camera.AdvancedCameraDirector.position(partialTick);
            setPosition(director.x, director.y, director.z);
            return;
        }
        var snapshot = BattleClient.snapshot();
        if (snapshot == null) return;
        double centerX = snapshot.arenaOriginX() + snapshot.arenaSize() * .5;
        double centerZ = snapshot.arenaOriginZ() + snapshot.arenaSize() * .5;
        var pan = BattleClient.cameraPan(); centerX += pan.x(); centerZ += pan.z();
        float yaw = DungeonPerspective.cameraYaw(), pitch = DungeonPerspective.cameraPitch();
        double distance = DungeonPerspective.zoomDistance();
        double yawRadians = Math.toRadians(yaw), pitchRadians = Math.toRadians(pitch), horizontal = Math.cos(pitchRadians);
        setPosition(centerX + Math.sin(yawRadians) * horizontal * distance,
                66 + Math.sin(pitchRadians) * distance,
                centerZ - Math.cos(yawRadians) * horizontal * distance);
    }
}
