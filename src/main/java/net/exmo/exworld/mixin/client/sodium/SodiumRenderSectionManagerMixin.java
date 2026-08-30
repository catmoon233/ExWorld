package net.exmo.exworld.mixin.client.sodium;

import net.exmo.exworld.client.ClientChunkGroupState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forces Sodium to rebuild its visibility graph when the server installs another irregular active group. */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
public abstract class SodiumRenderSectionManagerMixin {
    @Unique private long exworld$lastRevision = Long.MIN_VALUE;

    @Shadow
    public abstract void markGraphDirty();

    @Inject(method = "update", at = @At("HEAD"))
    private void exworld$refreshGraphAtChunkGroupBoundary(CallbackInfo ci) {
        long revision = ClientChunkGroupState.revision();
        if (revision == exworld$lastRevision) return;
        exworld$lastRevision = revision;
        markGraphDirty();
    }
}
