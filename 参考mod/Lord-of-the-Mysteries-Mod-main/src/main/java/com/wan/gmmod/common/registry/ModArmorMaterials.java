package com.wan.gmmod.common.registry;

import com.wan.gmmod.GuimiMod;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, GuimiMod.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TOP_HAT =
            ARMOR_MATERIALS.register("top_hat",
                    () -> new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                map.put(ArmorItem.Type.BOOTS, 0);
                                map.put(ArmorItem.Type.LEGGINGS, 0);
                                map.put(ArmorItem.Type.CHESTPLATE, 0);
                                map.put(ArmorItem.Type.HELMET, 0);
                            }),
                            0,
                            SoundEvents.ARMOR_EQUIP_LEATHER,
                            () -> Ingredient.EMPTY,
                            List.of(),
                            0.0F,
                            0.0F
                    )
            );

    /**
     * 马甲盔甲材料
     * <p>
     * 装填于胸甲槽，防御值设为 0（装饰性盔甲），
     * 后续可按需调整防御值、韧性、击退抗性等参数。
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> VEST =
            ARMOR_MATERIALS.register("vest",
                    () -> new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                map.put(ArmorItem.Type.BOOTS, 0);
                                map.put(ArmorItem.Type.LEGGINGS, 0);
                                map.put(ArmorItem.Type.CHESTPLATE, 0);
                                map.put(ArmorItem.Type.HELMET, 0);
                            }),
                            0,
                            SoundEvents.ARMOR_EQUIP_LEATHER,
                            () -> Ingredient.EMPTY,
                            List.of(),
                            0.0F,
                            0.0F
                    )
            );

    /**
     * 黑色长裤盔甲材料
     * <p>
     * 装填于护腿槽，防御值设为 0（装饰性盔甲），
     * 后续可按需调整防御值、韧性、击退抗性等参数。
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LONG_PANTS =
            ARMOR_MATERIALS.register("long_pants",
                    () -> new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                map.put(ArmorItem.Type.BOOTS, 0);
                                map.put(ArmorItem.Type.LEGGINGS, 0);
                                map.put(ArmorItem.Type.CHESTPLATE, 0);
                                map.put(ArmorItem.Type.HELMET, 0);
                            }),
                            0,
                            SoundEvents.ARMOR_EQUIP_LEATHER,
                            () -> Ingredient.EMPTY,
                            List.of(),
                            0.0F,
                            0.0F
                    )
            );

    /**
     * 黑皮鞋盔甲材料
     * <p>
     * 装填于靴子槽，防御值设为 0（装饰性盔甲），
     * 后续可按需调整防御值、韧性、击退抗性等参数。
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BLACK_SHOES =
            ARMOR_MATERIALS.register("black_shoes",
                    () -> new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                map.put(ArmorItem.Type.BOOTS, 0);
                                map.put(ArmorItem.Type.LEGGINGS, 0);
                                map.put(ArmorItem.Type.CHESTPLATE, 0);
                                map.put(ArmorItem.Type.HELMET, 0);
                            }),
                            0,
                            SoundEvents.ARMOR_EQUIP_LEATHER,
                            () -> Ingredient.EMPTY,
                            List.of(),
                            0.0F,
                            0.0F
                    )
            );

    /**
     * 黑色马甲（白衬）盔甲材料
     * <p>
     * 装填于胸甲槽，防御值设为 0（装饰性盔甲），
     * 后续可按需调整防御值、耐久、击退抗性等参数。
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BLACK_VEST =
            ARMOR_MATERIALS.register("black_vest",
                    () -> new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                map.put(ArmorItem.Type.BOOTS, 0);
                                map.put(ArmorItem.Type.LEGGINGS, 0);
                                map.put(ArmorItem.Type.CHESTPLATE, 0);
                                map.put(ArmorItem.Type.HELMET, 0);
                            }),
                            0,
                            SoundEvents.ARMOR_EQUIP_LEATHER,
                            () -> Ingredient.EMPTY,
                            List.of(),
                            0.0F,
                            0.0F
                    )
            );

    /**
     * 黎明铠甲材料
     * <p>
     * 装填于胸甲槽，使用 GeckoLib 渲染 3D 模型。
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DAWN =
            ARMOR_MATERIALS.register("dawn",
                    () -> new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                map.put(ArmorItem.Type.BOOTS, 0);
                                map.put(ArmorItem.Type.LEGGINGS, 0);
                                map.put(ArmorItem.Type.CHESTPLATE, 6);
                                map.put(ArmorItem.Type.HELMET, 0);
                            }),
                            12,
                            SoundEvents.ARMOR_EQUIP_NETHERITE,
                            () -> Ingredient.EMPTY,
                            List.of(),
                            2.0F,
                            0.05F
                    )
            );
}