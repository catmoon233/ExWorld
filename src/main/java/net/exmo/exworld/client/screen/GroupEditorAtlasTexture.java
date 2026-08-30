package net.exmo.exworld.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.model.MapTile;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * Translucent group-colour overlay atlas. It is drawn on top of the biome atlas so the editor shows the world's
 * rough appearance (biome colours) while still marking which tiles belong to which group.
 */
final class GroupEditorAtlasTexture implements AutoCloseable {
    /** Alpha of the group tint; low enough that the biome colour underneath stays readable. */
    private static final int TINT_ALPHA = 0x52;

    private final ResourceLocation location;

    GroupEditorAtlasTexture(WorldSnapshot snapshot, Map<String, String> tileOwners, Map<String, Boolean> configured) {
        NativeImage image = new NativeImage(snapshot.mapWidth(), snapshot.mapHeight(), false);
        try {
            for (MapTile tile : snapshot.tiles()) {
                String groupId = tileOwners.getOrDefault(tile.id(), tile.regionId());
                int rgb = WorldGroupEditorScreen.groupColor(groupId);
                if (!configured.getOrDefault(groupId, false)) rgb = desaturate(rgb);
                image.setPixelRGBA(tile.mapX() - snapshot.mapMinimumX(), tile.mapZ() - snapshot.mapMinimumZ(),
                        toAbgr((TINT_ALPHA << 24) | rgb));
            }
        } catch (RuntimeException exception) {
            image.close();
            throw exception;
        }
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(false, false);
        texture.upload();
        location = ResourceLocation.fromNamespaceAndPath(Exworld.MODID,
                "dynamic/world_groups_" + Integer.toUnsignedString(System.identityHashCode(this)));
        Minecraft.getInstance().getTextureManager().register(location, texture);
    }

    ResourceLocation location() { return location; }

    @Override public void close() { Minecraft.getInstance().getTextureManager().release(location); }

    private static int desaturate(int rgb) {
        int red = (rgb >> 16) & 0xFF, green = (rgb >> 8) & 0xFF, blue = rgb & 0xFF;
        int gray = (red * 30 + green * 59 + blue * 11) / 100;
        red = (red + gray * 2) / 3; green = (green + gray * 2) / 3; blue = (blue + gray * 2) / 3;
        return red << 16 | green << 8 | blue;
    }

    /** Converts 0xAARRGGBB to NativeImage's little-endian 0xAABBGGRR layout. */
    private static int toAbgr(int rgba) {
        return (rgba & 0xFF000000) | (rgba & 0x00FF0000) >> 16 | (rgba & 0x0000FF00) | (rgba & 0x000000FF) << 16;
    }
}
