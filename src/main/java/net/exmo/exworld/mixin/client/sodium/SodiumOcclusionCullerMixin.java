package net.exmo.exworld.mixin.client.sodium;

import net.exmo.exworld.client.ChunkGroupRenderCuller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sodium 0.8.x visibility-graph gate, adapted from StarRailExpress2's OcclusionCuller seam. */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller", remap = false)
public abstract class SodiumOcclusionCullerMixin {
    @Inject(method = {
            "isWithinFrustum(Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;)Z",
            "isWithinNearbySectionFrustum(Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;)Z"
    }, at = @At("HEAD"), cancellable = true)
    private static void exworld$rejectSectionsOutsideCurrentGroup(
            @Coerce Object viewport,
            @Coerce Object section,
            CallbackInfoReturnable<Boolean> cir
    ) {
        SodiumRenderSectionAccessor origin = (SodiumRenderSectionAccessor) section;
        if (!ChunkGroupRenderCuller.containsSection(origin.exworld$getOriginX(), origin.exworld$getOriginZ())) {
            cir.setReturnValue(false);
        }
    }
}
