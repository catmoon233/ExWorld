package net.exmo.exworld.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.exmo.exworld.world.model.MapTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** One tiny client-side biome-colour atlas replaces terrain sampling and the 512×512 terrain texture upload. */
final class BiomeAtlasTexture implements AutoCloseable {
    private final ResourceLocation location;

    BiomeAtlasTexture(WorldSnapshot snapshot) {
        NativeImage image = new NativeImage(snapshot.mapWidth(), snapshot.mapHeight(), false);
        try {
            for (MapTile tile : snapshot.tiles()) {
                int color = 0xFF000000 | tile.biome().mapColor();
                int abgr = color & 0xFF00FF00 | (color & 0x00FF0000) >> 16 | (color & 0x000000FF) << 16;
                image.setPixelRGBA(tile.mapX() - snapshot.mapMinimumX(), tile.mapZ() - snapshot.mapMinimumZ(), abgr);
            }
        } catch (RuntimeException exception) {
            image.close();
            throw exception;
        }
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(false, false);
        texture.upload();
        location = ResourceLocation.fromNamespaceAndPath(Exworld.MODID,
                "dynamic/world_biomes_" + Integer.toUnsignedString(System.identityHashCode(snapshot)));
        Minecraft.getInstance().getTextureManager().register(location, texture);
    }

    ResourceLocation location() { return location; }

    @Override public void close() { Minecraft.getInstance().getTextureManager().release(location); }
}
