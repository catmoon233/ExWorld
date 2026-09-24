package io.redspace.irons_artifice.datagen;

import io.redspace.irons_artifice.IronsArtifice;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

/** PORT-BLOCKED: 1.21.1 has no 26.1 item-model datagen. Converted models already live under assets. */
public class ItemModelDataGenerator implements DataProvider {
    public ItemModelDataGenerator(PackOutput output) {
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getName() {
        return IronsArtifice.MODID + "_item_models";
    }
}
