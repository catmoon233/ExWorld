package net.exmo.exworld.world.generation;

import net.exmo.exworld.Exworld;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/** Aether-style noise settings: island density + surface rules, no aquifers or ore veins. */
public final class ArchipelagoNoiseBuilders {
    public static final ResourceKey<DensityFunction> ISLAND_FIELD = ResourceKey.create(Registries.DENSITY_FUNCTION,
            ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "island_field"));
    public static final ResourceKey<NoiseGeneratorSettings> ARCHIPELAGO_NOISE = ResourceKey.create(Registries.NOISE_SETTINGS,
            ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "archipelago"));

    private ArchipelagoNoiseBuilders() {}

    public static void bootstrapDensity(BootstrapContext<DensityFunction> context) {
        context.register(ISLAND_FIELD, new IslandField(0L));
    }

    public static void bootstrapNoiseSettings(BootstrapContext<NoiseGeneratorSettings> context) {
        HolderGetter<DensityFunction> density = context.lookup(Registries.DENSITY_FUNCTION);
        HolderGetter<NormalNoise.NoiseParameters> noise = context.lookup(Registries.NOISE);
        context.register(ARCHIPELAGO_NOISE, archipelago(density, noise));
    }

    public static NoiseGeneratorSettings archipelago(HolderGetter<DensityFunction> densityFunctions,
                                                     HolderGetter<NormalNoise.NoiseParameters> noise) {
        return new NoiseGeneratorSettings(
                NoiseSettings.create(0, 320, 1, 1),
                Blocks.STONE.defaultBlockState(),
                Blocks.WATER.defaultBlockState(),
                router(densityFunctions, noise),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
                                        SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())),
                                SurfaceRules.state(Blocks.DIRT.defaultBlockState()))),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.state(Blocks.DIRT.defaultBlockState()))),
                List.of(),
                -64,
                false,
                false,
                false,
                false);
    }

    private static NoiseRouter router(HolderGetter<DensityFunction> densityFunctions,
                                      HolderGetter<NormalNoise.NoiseParameters> noise) {
        DensityFunction shiftX = function(densityFunctions, "shift_x");
        DensityFunction shiftZ = function(densityFunctions, "shift_z");
        DensityFunction temperature = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noise.getOrThrow(Noises.TEMPERATURE));
        DensityFunction vegetation = DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noise.getOrThrow(Noises.VEGETATION));
        DensityFunction finalDensity = buildFinalDensity(densityFunctions, noise);
        return new NoiseRouter(
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                temperature,
                vegetation,
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                finalDensity,
                DensityFunctions.zero(),
                DensityFunctions.zero(),
                DensityFunctions.zero());
    }

    private static DensityFunction buildFinalDensity(HolderGetter<DensityFunction> densityFunctions,
                                                     HolderGetter<NormalNoise.NoiseParameters> noise) {
        DensityFunction density = new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(ISLAND_FIELD));
        density = DensityFunctions.add(density, DensityFunctions.mul(DensityFunctions.constant(0.05),
                DensityFunctions.noise(noise.getOrThrow(Noises.CAVE_CHEESE), 0.6, 0.6)));
        density = slide(density, 0, 320, 24, 0, -0.2, 0, 12, -0.15);
        density = DensityFunctions.blendDensity(density);
        density = DensityFunctions.interpolated(density);
        return density.squeeze();
    }

    private static DensityFunction slide(DensityFunction density, int minY, int maxY, int fromYTop, int toYTop,
                                         double topOffset, int fromYBottom, int toYBottom, double bottomOffset) {
        DensityFunction topSlide = DensityFunctions.yClampedGradient(minY + maxY - fromYTop, minY + maxY - toYTop, 1, 0);
        density = DensityFunctions.lerp(topSlide, topOffset, density);
        DensityFunction bottomSlide = DensityFunctions.yClampedGradient(minY + fromYBottom, minY + toYBottom, 0, 1);
        return DensityFunctions.lerp(bottomSlide, bottomOffset, density);
    }

    private static DensityFunction function(HolderGetter<DensityFunction> densityFunctions, String vanillaName) {
        return new DensityFunctions.HolderHolder(densityFunctions.getOrThrow(
                ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(vanillaName))));
    }
}
