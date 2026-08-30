package net.exmo.exworld.content.block;

import net.exmo.exworld.content.ExWorldContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Configurable dungeon entrance; the block stores the data-driven dungeon id. */
public final class DungeonEntranceBlockEntity extends BlockEntity {
    private String dungeonId = "exworld:training_dungeon";
    public DungeonEntranceBlockEntity(BlockPos pos, BlockState state) { super(ExWorldContent.DUNGEON_ENTRANCE_ENTITY.get(), pos, state); }
    public String dungeonId() { return dungeonId; }
    public void dungeonId(String value) { dungeonId = value == null || value.isBlank() ? "exworld:training_dungeon" : value; setChanged(); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.putString("dungeon_id", dungeonId); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); dungeonId = tag.contains("dungeon_id") ? tag.getString("dungeon_id") : "exworld:training_dungeon"; }
}
