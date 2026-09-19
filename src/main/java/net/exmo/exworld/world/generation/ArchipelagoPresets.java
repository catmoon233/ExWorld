package net.exmo.exworld.world.generation;

import net.exmo.exworld.Exworld;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Map;

/** World type {@code exworld:archipelago} — overworld uses island noise, nether/end stay vanilla. */
public final class ArchipelagoPresets {
    public static final ResourceKey<WorldPreset> ARCHIPELAGO = ResourceKey.create(Registries.WORLD_PRESET,
            ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "archipelago"));

    private ArchipelagoPresets() {}

    public static void bootstrap(BootstrapContext<WorldPreset> context) {
        HolderGetter<DimensionType> dimensionTypes = context.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<NoiseGeneratorSettings> noiseSettings = context.lookup(Registries.NOISE_SETTINGS);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<MultiNoiseBiomeSourceParameterList> parameters =
                context.lookup(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);

        Holder<DimensionType> overworldType = dimensionTypes.getOrThrow(BuiltinDimensionTypes.OVERWORLD);
        BiomeSource overworldBiomes = MultiNoiseBiomeSource.createFromPreset(
                parameters.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
        LevelStem overworld = new LevelStem(overworldType,
                new NoiseBasedChunkGenerator(overworldBiomes, noiseSettings.getOrThrow(ArchipelagoNoiseBuilders.ARCHIPELAGO_NOISE)));

        Holder<DimensionType> netherType = dimensionTypes.getOrThrow(BuiltinDimensionTypes.NETHER);
        LevelStem nether = new LevelStem(netherType, new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromPreset(parameters.getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER)),
                noiseSettings.getOrThrow(NoiseGeneratorSettings.NETHER)));

        Holder<DimensionType> endType = dimensionTypes.getOrThrow(BuiltinDimensionTypes.END);
        LevelStem end = new LevelStem(endType, new NoiseBasedChunkGenerator(
                TheEndBiomeSource.create(biomes), noiseSettings.getOrThrow(NoiseGeneratorSettings.END)));

        context.register(ARCHIPELAGO, new WorldPreset(Map.of(
                LevelStem.OVERWORLD, overworld,
                LevelStem.NETHER, nether,
                LevelStem.END, end)));
    }

    public static boolean isArchipelago(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) return false;
        return noise.generatorSettings().unwrapKey()
                .filter(key -> key.equals(ArchipelagoNoiseBuilders.ARCHIPELAGO_NOISE))
                .isPresent();
    }
}
