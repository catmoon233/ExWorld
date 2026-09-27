package com.wan.gmmod.content.altar;

import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.talisman.TalismanData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 祭台配方注册表：集中存储所有祭台合成配方。
 * <p>
 * 通过 {@link #register(AltarRecipe)} 追加配方，{@link #init()} 在通用初始化阶段注册内置配方。
 */
public final class AltarRecipeManager {

    private static final List<AltarRecipe> RECIPES = new ArrayList<>();

    /** 灵性符咒的合成灵性消耗（基础 30 点）。 */
    public static final int TALISMAN_SPIRIT_COST = 30;

    private AltarRecipeManager() {}

    /** 追加一条配方。 */
    public static void register(AltarRecipe recipe) {
        RECIPES.add(recipe);
    }

    /**
     * 依据祭台上材料匹配配方，返回首个满足的配方；无匹配返回 {@code null}。
     */
    public static AltarRecipe findMatch(Map<Item, Integer> available) {
        for (AltarRecipe recipe : RECIPES) {
            if (recipe.matches(available)) {
                return recipe;
            }
        }
        return null;
    }

    /** 注册内置祭台配方。幂等：重复调用会先清空既有配方。 */
    public static void init() {
        RECIPES.clear();

        // 小丑 / 魔术师 / 无面人 / 秘偶大师魔药均已迁至炼药系统（BrewingRecipeManager）。
        // 祭台拥有独立的仪式合成系统：需在灵性之墙内、祭台周围摆放蜡烛x3才能合成。
        // 此处追加仪式专属配方：灵性符咒（空白符咒 + 对应途径素材，消耗 30 点灵性）。

        // 净化符咒（祈求太阳）：空白符咒 + 驱邪子弹 + 龙牙草
        register(AltarRecipe.of(
                Map.of(ModItems.BLANK_TALISMAN.get(), 1,
                        ModItems.EXORCISM_BULLET.get(), 1,
                        ModItems.AGRIMONY.get(), 1),
                () -> talisman(ModItems.PURIFICATION_TALISMAN.get(), "sun", "purification"),
                TALISMAN_SPIRIT_COST));

        // 安魂符咒（祈求黑夜）：空白符咒 + 古老怨灵残余灵性 + 黑色曼陀罗
        register(AltarRecipe.of(
                Map.of(ModItems.BLANK_TALISMAN.get(), 1,
                        ModItems.ANCIENT_WRAITH_RESIDUAL_SPIRITUALITY.get(), 1,
                        ModItems.BLACK_MANDRAGORA.get(), 1),
                () -> talisman(ModItems.REQUIEM_TALISMAN.get(), "night", "requiem"),
                TALISMAN_SPIRIT_COST));

        // 电击符咒（祈求暴君）：空白符咒 + 星水晶 + 火药
        register(AltarRecipe.of(
                Map.of(ModItems.BLANK_TALISMAN.get(), 1,
                        ModItems.STAR_CRYSTAL.get(), 1,
                        Items.GUNPOWDER, 1),
                () -> talisman(ModItems.ELECTRIC_TALISMAN.get(), "tyrant", "electric"),
                TALISMAN_SPIRIT_COST));
    }

    /** 构造带符咒数据的成品堆叠。 */
    private static ItemStack talisman(Item item, String deity, String type) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.TALISMAN.get(), new TalismanData(deity, type));
        return stack;
    }
}
