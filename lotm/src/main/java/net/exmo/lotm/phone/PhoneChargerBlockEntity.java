package net.exmo.lotm.phone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PhoneChargerBlockEntity extends BlockEntity {
    private ItemStack phone = ItemStack.EMPTY;

    public PhoneChargerBlockEntity(BlockPos pos, BlockState state) {
        super(LotmBlocks.CHARGER_ENTITY.get(), pos, state);
    }

    public boolean insert(ItemStack stack) {
        if (!phone.isEmpty() || !(stack.getItem() instanceof PhoneItem) || stack.isEmpty()) return false;
        phone = stack.copyWithCount(1);
        setChanged();
        return true;
    }

    public ItemStack extract() {
        ItemStack out = phone;
        phone = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    public void drop() {
        if (level == null || phone.isEmpty()) return;
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), phone);
        phone = ItemStack.EMPTY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhoneChargerBlockEntity charger) {
        if (charger.phone.isEmpty() || level.getGameTime() % 20 != 0) return;
        if (PhoneStacks.battery(charger.phone) >= 100) return;
        PhoneStacks.addBattery(charger.phone, 1);
        charger.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!phone.isEmpty()) tag.put("phone", phone.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        phone = tag.contains("phone") ? ItemStack.parse(registries, tag.getCompound("phone")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
    }
}
