package com.wan.gmmod.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wan.gmmod.content.characteristics.CharacteristicManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * 全局战利品修改器（GLM）：将非凡特性物品注入宝箱战利品表。
 * <p>
 * 仅作用于 {@code chests/} 前缀的战利品表；实际是否掉落由守恒定律的「未分配池」决定
 * （见 {@link CharacteristicManager#drawFromPending}）——未分配池耗尽后不再发放，
 * 从而保证世界中物理散布的特性总量守恒。触发概率由 JSON 中的 {@code random_chance} 条件控制。
 */
public class CharacteristicChestModifier extends LootModifier {
    public static final MapCodec<CharacteristicChestModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).apply(inst, CharacteristicChestModifier::new));

    protected CharacteristicChestModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        if (table == null || !table.getPath().startsWith("chests/")) {
            return generatedLoot;
        }
        ItemStack characteristic = CharacteristicManager.drawFromPending(context.getLevel(), context.getRandom());
        if (characteristic != null && !characteristic.isEmpty()) {
            generatedLoot.add(characteristic);
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
