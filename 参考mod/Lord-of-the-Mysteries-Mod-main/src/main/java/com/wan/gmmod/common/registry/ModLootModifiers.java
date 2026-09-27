package com.wan.gmmod.common.registry;

import com.mojang.serialization.MapCodec;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.loot.CharacteristicChestModifier;
import com.wan.gmmod.common.loot.GuaranteedCharacteristicModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 全局战利品修改器（Global Loot Modifier）序列化器注册。
 */
public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, GuimiMod.MODID);

    /** 特性注入宝箱修改器。 */
    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> CHARACTERISTIC_CHEST =
            GLM.register("characteristic_chest", () -> CharacteristicChestModifier.CODEC);

    /** 遗迹固定特性注入修改器。 */
    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> GUARANTEED_CHEST =
            GLM.register("guaranteed_chest", () -> GuaranteedCharacteristicModifier.CODEC);

    public static void register(IEventBus bus) {
        GLM.register(bus);
    }
}
