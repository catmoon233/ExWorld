package net.exmo.exworld.data;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.generation.ArchipelagoNoiseBuilders;
import net.exmo.exworld.world.generation.ArchipelagoPresets;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ExWorldRegistrySets extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DENSITY_FUNCTION, ArchipelagoNoiseBuilders::bootstrapDensity)
            .add(Registries.NOISE_SETTINGS, ArchipelagoNoiseBuilders::bootstrapNoiseSettings)
            .add(Registries.WORLD_PRESET, ArchipelagoPresets::bootstrap);

    public ExWorldRegistrySets(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(Exworld.MODID));
    }
}
