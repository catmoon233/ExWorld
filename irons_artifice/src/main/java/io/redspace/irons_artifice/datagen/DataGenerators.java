package io.redspace.irons_artifice.datagen;

import io.redspace.irons_artifice.IronsArtifice;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = IronsArtifice.MODID)
public final class DataGenerators {
    private DataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        if (event.includeClient()) {
            generator.addProvider(true, new ItemModelDataGenerator(output));
        }
        if (event.includeServer()) {
            generator.addProvider(true, new RecipeDataGenerator(output, lookup));
            generator.addProvider(true, new ItemTagDataGenerator(output, lookup));
            generator.addProvider(true, new BlockTagDataGenerator(output, lookup));
            generator.addProvider(true, new EntityTypeTagDataGenerator(output, lookup));
            generator.addProvider(true, new LootTableDataGenerator(output, lookup));
            generator.addProvider(true, new AdvancementDataGenerator(output, lookup));
        }
    }
}
