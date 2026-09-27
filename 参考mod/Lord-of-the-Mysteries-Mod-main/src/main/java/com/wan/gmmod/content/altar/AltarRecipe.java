package com.wan.gmmod.content.altar;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 祭台配方：材料放置在祭台上，空手右键触发合成。
 *
 * @param ingredients 材料物品 → 所需最小数量
 * @param result      合成产物提供器
 * @param spiritCost  合成所需消耗的灵性值（由施法玩家支付）
 */
public record AltarRecipe(Map<Item, Integer> ingredients, Supplier<ItemStack> result, int spiritCost) {

    /** 便捷工厂：无灵性消耗的普通配方。 */
    public static AltarRecipe of(Map<Item, Integer> ingredients, Supplier<ItemStack> result) {
        return new AltarRecipe(ingredients, result, 0);
    }

    /** 便捷工厂：带灵性消耗的配方（如灵性符咒）。 */
    public static AltarRecipe of(Map<Item, Integer> ingredients, Supplier<ItemStack> result, int spiritCost) {
        return new AltarRecipe(ingredients, result, spiritCost);
    }

    /**
     * 判断祭台上可用材料是否满足本配方。
     */
    public boolean matches(Map<Item, Integer> available) {
        for (Map.Entry<Item, Integer> entry : ingredients.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** 产出一份新的成品堆叠。 */
    public ItemStack createResult() {
        return result.get();
    }
}
