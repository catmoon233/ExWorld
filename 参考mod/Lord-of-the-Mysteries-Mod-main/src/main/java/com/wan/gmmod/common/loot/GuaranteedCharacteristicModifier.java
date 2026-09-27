package com.wan.gmmod.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.wan.gmmod.Config;
import com.wan.gmmod.content.characteristics.CharacteristicManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.Set;

/**
 * 遗迹固定生成：古老神殿 / 古代都市等灵界遗迹宝箱必定尝试生成一份非凡特性
 * （守恒池耗尽则跳过），与 {@link CharacteristicChestModifier} 的概率注入互补。
 */
public class GuaranteedCharacteristicModifier extends LootModifier {
    public static final MapCodec<GuaranteedCharacteristicModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).apply(inst, GuaranteedCharacteristicModifier::new));

    private static final Set<String> RUIN_TABLES = Set.of(
            "chests/ancient_city",
            "chests/ruined_portal",
            "chests/underwater_ruin_big",
            "chests/desert_pyramid",
            "chests/jungle_temple"
    );

    protected GuaranteedCharacteristicModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!Config.GUARANTEED_CHEST_ENABLED.get()) {
            return generatedLoot;
        }
        ResourceLocation table = context.getQueriedLootTableId();
        if (table == null || !RUIN_TABLES.contains(table.getPath())) {
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