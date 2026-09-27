package com.wan.gmmod.common.block.entity;

import com.wan.gmmod.common.registry.ModBlockEntities;
import com.wan.gmmod.content.altar.AltarRecipe;
import com.wan.gmmod.content.altar.AltarRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 祭台方块实体：存储放置在祭台上的材料物品，并管理配方匹配与合成逻辑。
 * <p>
 * 最大容量 16 个物品堆叠。支持序列化到 NBT 以随世界保存。
 */
public class AltarBlockEntity extends BlockEntity {

    /** 祭台最大材料槽位数 */
    private static final int MAX_SLOTS = 16;

    /** 存储放置在祭台上的物品 */
    private final List<ItemStack> items = new ArrayList<>();

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR.get(), pos, state);
    }

    /** 放入物品，成功返回 true。 */
    public boolean addItem(ItemStack stack) {
        if (items.size() >= MAX_SLOTS || stack.isEmpty()) {
            return false;
        }
        items.add(stack.copy());
        setChanged();
        return true;
    }

    /** 移除最后放入的物品。 */
    public ItemStack removeLastItem() {
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = items.remove(items.size() - 1);
        setChanged();
        return removed;
    }

    /** 是否有放置的物品。 */
    public boolean hasItems() {
        return !items.isEmpty();
    }

    /**
     * 仅判断当前材料是否能匹配某个配方（不消耗材料）。
     */
    public boolean hasMatchingRecipe() {
        return matchingRecipe() != null;
    }

    /**
     * 返回当前材料匹配到的配方；无匹配返回 {@code null}。
     */
    public AltarRecipe matchingRecipe() {
        Map<Item, Integer> available = new HashMap<>();
        for (ItemStack stack : items) {
            available.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return AltarRecipeManager.findMatch(available);
    }

    /**
     * 尝试配方匹配并合成。
     * 成功则消耗材料并返回产物；失败返回空。
     */
    public ItemStack tryCraft() {
        // 统计祭台上的物品
        Map<Item, Integer> available = new HashMap<>();
        for (ItemStack stack : items) {
            available.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        AltarRecipe recipe = AltarRecipeManager.findMatch(available);
        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        // 消耗材料
        Map<Item, Integer> need = new HashMap<>(recipe.ingredients());
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack stack : items) {
            Item item = stack.getItem();
            int req = need.getOrDefault(item, 0);
            if (req > 0) {
                int take = Math.min(req, stack.getCount());
                need.put(item, req - take);
                if (stack.getCount() > take) {
                    ItemStack leftover = stack.copy();
                    leftover.setCount(stack.getCount() - take);
                    remaining.add(leftover);
                }
            } else {
                remaining.add(stack.copy());
            }
        }

        items.clear();
        items.addAll(remaining);
        setChanged();
        return recipe.createResult();
    }

    /** 掉落全部物品（方块被破坏时调用）。 */
    public void dropAllItems(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ItemEntity entity = new ItemEntity(level,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
                level.addFreshEntity(entity);
            }
        }
        items.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag listTag = new ListTag();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                listTag.add(stack.save(registries));
            }
        }
        tag.put("AltarItems", listTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        if (tag.contains("AltarItems", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("AltarItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                ItemStack stack = ItemStack.parse(registries, listTag.getCompound(i)).orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            }
        }
    }
}
