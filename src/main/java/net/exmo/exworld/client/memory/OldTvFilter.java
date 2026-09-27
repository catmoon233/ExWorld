package net.exmo.exworld.client.memory;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.mixin.client.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

/** Toggles the 1.21.1 post chain while a memory is playing. */
public final class OldTvFilter {
    public static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "shaders/post/old_tv.json");
    private static boolean wanted;
    private static boolean warned;

    private OldTvFilter() {}

    public static boolean wanted() { return wanted; }

    public static void enable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) return;
        wanted = true;
        try {
            minecraft.gameRenderer.loadEffect(LOCATION);
        } catch (RuntimeException ex) {
            wanted = false;
            warn(ex);
        }
    }

    public static void disable() {
        if (!wanted) return;
        wanted = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) return;
        try {
            minecraft.gameRenderer.shutdownEffect();
        } catch (RuntimeException ignored) {
            // The effect was already gone.
        }
    }

    public static boolean loaded() {
        return wanted && chain() != null;
    }

    public static void tick(float seconds) {
        if (!wanted) return;
        try {
            PostChain chain = chain();
            if (chain == null) {
                if (!warned) warn(null);
                return;
            }
            chain.setUniform("Time", seconds);
        } catch (RuntimeException ignored) {
            // A vanilla program in the chain may not declare Time.
        }
    }

    private static PostChain chain() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) return null;
        return ((GameRendererAccessor) minecraft.gameRenderer).exworld$postEffect();
    }

    private static void warn(RuntimeException ex) {
        if (warned) return;
        warned = true;
        if (ex == null) Exworld.LOGGER.warn("Old TV memory shader did not load; using the scanline overlay");
        else Exworld.LOGGER.warn("Old TV memory shader failed; using the scanline overlay", ex);
    }
}
