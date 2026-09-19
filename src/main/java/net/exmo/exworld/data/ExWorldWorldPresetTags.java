package net.exmo.exworld.data;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.generation.ArchipelagoPresets;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.WorldPresetTagsProvider;
import net.minecraft.tags.WorldPresetTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class ExWorldWorldPresetTags extends WorldPresetTagsProvider {
    public ExWorldWorldPresetTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                  @Nullable ExistingFileHelper files) {
        super(output, lookup, Exworld.MODID, files);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(WorldPresetTags.NORMAL).add(ArchipelagoPresets.ARCHIPELAGO);
        tag(WorldPresetTags.EXTENDED).add(ArchipelagoPresets.ARCHIPELAGO);
    }
}
