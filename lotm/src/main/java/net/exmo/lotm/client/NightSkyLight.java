package net.exmo.lotm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

/**
 * Faded overworld night red, using Enhanced Celestials' lightmap and moon-color path.
 * A depth-masked post shader tears at the horizon because sky and distant terrain share the far plane.
 */
public final class NightSkyLight {
    // EC blood moon fully replaces sky light with 0x990000 and paints the moon the same color.
    private static final float SKY_LIGHT_BLEND = 0.70F;
    private static final Vector3f SKY_LIGHT_COLOR = new Vector3f(0.56F, 0.07F, 0.05F);
    private static final float MOON_BLEND = 0.58F;
    private static final float MOON_RED = 0.70F;
    private static final float MOON_GREEN = 0.24F;
    private static final float MOON_BLUE = 0.20F;
    private static final float DOME_BLEND = 0.18F;
    private static final float DOME_RED = 0.42F;
    private static final float DOME_GREEN = 0.09F;
    private static final float DOME_BLUE = 0.07F;

    private NightSkyLight() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NightSkyLight::fog);
    }

    public static float nightBlend(float skyDarken, float rain) {
        float daylight = (skyDarken - 0.2F) / 0.8F;
        return Mth.clamp((1.0F - daylight) - rain, 0.0F, 1.0F);
    }

    public static void tintSkyLight(Vector3f skyVector, float skyDarken, float rain) {
        float amount = nightBlend(skyDarken, rain) * SKY_LIGHT_BLEND;
        if (amount <= 0.0F) return;
        skyVector.lerp(SKY_LIGHT_COLOR, amount);
    }

    public static void tintCapturedSkyLight(Vector3f skyVector) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!overworld(level)) return;
        tintSkyLight(skyVector, level.getSkyDarken(1.0F), level.getRainLevel(1.0F));
    }

    public static Vec3 tintDome(Vec3 color, float skyDarken, float rain) {
        float amount = nightBlend(skyDarken, rain) * DOME_BLEND;
        if (amount <= 0.0F) return color;
        return color.lerp(new Vec3(DOME_RED, DOME_GREEN, DOME_BLUE), amount);
    }

    public static Vector3f moonColor(float skyDarken, float rain) {
        float amount = nightBlend(skyDarken, rain) * MOON_BLEND;
        return new Vector3f(
                Mth.lerp(amount, 1.0F, MOON_RED),
                Mth.lerp(amount, 1.0F, MOON_GREEN),
                Mth.lerp(amount, 1.0F, MOON_BLUE));
    }

    public static void tintMoon(float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!overworld(level)) return;
        float rain = level.getRainLevel(partialTick);
        if (nightBlend(level.getSkyDarken(partialTick), rain) <= 0.0F) return;
        Vector3f color = moonColor(level.getSkyDarken(partialTick), rain);
        RenderSystem.setShaderColor(color.x(), color.y(), color.z(), 1.0F - rain);
    }

    private static void fog(ViewportEvent.ComputeFogColor event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!overworld(level) || event.getCamera().getFluidInCamera() != FogType.NONE) return;
        float partialTick = (float) event.getPartialTick();
        float amount = nightBlend(level.getSkyDarken(partialTick), level.getRainLevel(partialTick)) * DOME_BLEND;
        if (amount <= 0.0F) return;
        event.setRed(Mth.lerp(amount, event.getRed(), DOME_RED));
        event.setGreen(Mth.lerp(amount, event.getGreen(), DOME_GREEN));
        event.setBlue(Mth.lerp(amount, event.getBlue(), DOME_BLUE));
    }

    private static boolean overworld(ClientLevel level) {
        return level != null && level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL;
    }
}
