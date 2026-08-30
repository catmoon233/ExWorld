package net.exmo.exworld.mixin;

import net.exmo.exworld.world.model.WorldDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the vanilla Overworld's real level-height contract 0..319 before levels and chunk sections are constructed.
 * Checking raw record fields keeps Nether, End, and custom dimensions untouched.
 */
@Mixin(DimensionType.class)
public abstract class DimensionTypeMixin {
    private static final int VANILLA_OVERWORLD_MIN_Y = -64;
    private static final int VANILLA_OVERWORLD_HEIGHT = 384;

    @Shadow @Final private int minY;
    @Shadow @Final private int height;
    @Shadow @Final private int logicalHeight;
    @Shadow @Final private ResourceLocation effectsLocation;

    @Inject(method = "minY", at = @At("HEAD"), cancellable = true)
    private void exworld$overworldMinY(CallbackInfoReturnable<Integer> callback) {
        if (exworld$isVanillaOverworld()) callback.setReturnValue(WorldDimensions.MIN_BUILD_HEIGHT);
    }

    @Inject(method = "height", at = @At("HEAD"), cancellable = true)
    private void exworld$overworldHeight(CallbackInfoReturnable<Integer> callback) {
        if (exworld$isVanillaOverworld()) callback.setReturnValue(WorldDimensions.BUILD_HEIGHT);
    }

    @Inject(method = "logicalHeight", at = @At("HEAD"), cancellable = true)
    private void exworld$overworldLogicalHeight(CallbackInfoReturnable<Integer> callback) {
        if (exworld$isVanillaOverworld()) callback.setReturnValue(WorldDimensions.BUILD_HEIGHT);
    }

    private boolean exworld$isVanillaOverworld() {
        return minY == VANILLA_OVERWORLD_MIN_Y && height == VANILLA_OVERWORLD_HEIGHT
                && logicalHeight == VANILLA_OVERWORLD_HEIGHT && effectsLocation.toString().equals("minecraft:overworld");
    }
}
