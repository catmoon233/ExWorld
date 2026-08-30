package net.exmo.exworld.mixin.client;

import net.exmo.exworld.client.ChunkGroupRenderCuller;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Removes sections outside the active chunk group from both full and incremental visibility results. */
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
    @Inject(method = "addSectionsInFrustum", at = @At("RETURN"))
    private void exworld$cullFullVisibilityResult(
            Frustum frustum,
            List<SectionRenderDispatcher.RenderSection> sections,
            CallbackInfo ci
    ) {
        sections.removeIf(section -> !ChunkGroupRenderCuller.contains(section.getOrigin()));
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void exworld$cullIncrementalVisibilityResult(
            boolean advancedCulling,
            Camera camera,
            Frustum frustum,
            List<SectionRenderDispatcher.RenderSection> sections,
            CallbackInfo ci
    ) {
        sections.removeIf(section -> !ChunkGroupRenderCuller.contains(section.getOrigin()));
    }
}
