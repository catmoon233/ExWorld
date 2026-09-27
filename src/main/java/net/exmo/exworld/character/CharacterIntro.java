package net.exmo.exworld.character;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent player biography imported from a markdown file. */
public final class CharacterIntro implements INBTSerializable<CompoundTag> {
    private String text = "";

    public String text() {
        return text;
    }

    public void text(String value) {
        text = value == null ? "" : value;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("text", text);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        text = tag.getString("text");
    }
}
