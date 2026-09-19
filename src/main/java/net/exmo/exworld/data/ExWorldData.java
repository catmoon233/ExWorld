package net.exmo.exworld.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

public final class ExWorldData {
    private ExWorldData() {}

    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExWorldRegistrySets registries = new ExWorldRegistrySets(output, lookup);
        generator.addProvider(event.includeServer(), registries);
        generator.addProvider(event.includeServer(),
                new ExWorldWorldPresetTags(output, registries.getRegistryProvider(), files));
    }
}
