//package io.redspace.irons_artifice.datagen;
//
//import io.redspace.irons_artifice.IronsArtifice;
//import io.redspace.irons_artifice.utils.IronsArtificeTags;
//import net.minecraft.core.HolderLookup;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.data.PackOutput;
//import net.minecraft.data.tags.KeyTagProvider;
//import net.minecraft.world.level.storage.loot.BuiltInLootTables;
//import net.minecraft.world.level.storage.loot.LootTable;
//
//import java.util.concurrent.CompletableFuture;
//
//public class LootTableTagGenerator extends KeyTagProvider<LootTable> {
//    public LootTableTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
//        super(output, Registries.LOOT_TABLE, lookupProvider, IronsArtifice.MODID);
//    }
//
//    @Override
//    protected void addTags(HolderLookup.Provider registries) {
//        this.tag(IronsArtificeTags.CURSED_BY_PIRATES)
//                .add(BuiltInLootTables.BURIED_TREASURE)
//                .add(BuiltInLootTables.SHIPWRECK_TREASURE)
//                .add(BuiltInLootTables.UNDERWATER_RUIN_BIG)
//        ;
//    }
//}
