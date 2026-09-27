package com.wan.gmmod.common.registry;

import com.mojang.serialization.Codec;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.ancient.AncientArtifactData;
import com.wan.gmmod.content.characteristics.CharacteristicData;
import com.wan.gmmod.content.characteristics.MagicArtifactData;
import com.wan.gmmod.content.characteristics.SealedArtifactData;
import com.wan.gmmod.content.talisman.TalismanData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * 数据组件注册。
 * <p>
 * {@link #CHARACTERISTIC} 挂在特性物品上，记录该物品承载的途径与序列等级，
 * 让所有途径的非凡特性共用同一种物品，仅靠组件数据区分。
 * {@link #SEALED_ARTIFACT} 挂在封印物上，记录来源特性与封印基底。
 */
public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GuimiMod.MODID);

    /** 非凡特性数据（途径 + 等级） */
    public static final Supplier<DataComponentType<CharacteristicData>> CHARACTERISTIC =
            DATA_COMPONENTS.registerComponentType("characteristic", builder -> builder
                    .persistent(CharacteristicData.CODEC)
                    .networkSynchronized(CharacteristicData.STREAM_CODEC));

    /** 封印物数据（途径 + 等级 + 基底物品） */
    public static final Supplier<DataComponentType<SealedArtifactData>> SEALED_ARTIFACT =
            DATA_COMPONENTS.registerComponentType("sealed_artifact", builder -> builder
                    .persistent(SealedArtifactData.CODEC)
                    .networkSynchronized(SealedArtifactData.STREAM_CODEC));

    /** 神奇物品数据（途径 + 等级 + 随机性变体 + 基底物品） */
    public static final Supplier<DataComponentType<MagicArtifactData>> MAGIC_ARTIFACT =
            DATA_COMPONENTS.registerComponentType("magic_artifact", builder -> builder
                    .persistent(MagicArtifactData.CODEC)
                    .networkSynchronized(MagicArtifactData.STREAM_CODEC));

    /** 丧钟弹巢内已装填的子弹数量 */
    public static final Supplier<DataComponentType<Integer>> GUN_AMMO =
            DATA_COMPONENTS.registerComponentType("gun_ammo", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** 丧钟弹巢内子弹类型队列（物品 ID，先装先发），决定每发子弹的命中效果 */
    public static final Supplier<DataComponentType<List<String>>> GUN_MAGAZINE =
            DATA_COMPONENTS.registerComponentType("gun_magazine", builder -> builder
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())));

    /** 魔杖替身绑定：绑定女巫的玩家 UUID 字符串。可把魔杖交给他人实现远程救援。 */
    public static final Supplier<DataComponentType<String>> WAND_BOND =
            DATA_COMPONENTS.registerComponentType("wand_bond", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /** 符咒数据（祈求对象 + 类型）：挂在三种灵性符咒上，合成时由祭台写入。 */
    public static final Supplier<DataComponentType<TalismanData>> TALISMAN =
            DATA_COMPONENTS.registerComponentType("talisman", builder -> builder
                    .persistent(TalismanData.CODEC)
                    .networkSynchronized(TalismanData.STREAM_CODEC));

    /** 符咒「灌注」到期游戏刻：灌注后 3 秒内可投掷，过期自动消散（发光标记随之移除）。 */
    public static final Supplier<DataComponentType<Long>> TALISMAN_CHARGE_END =
            DATA_COMPONENTS.registerComponentType("talisman_charge_end", builder -> builder
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** 货币价值（以便士为最小单位）：1 便士 = 1，1 苏勒 = 12，1 金镑 = 240。 */
    public static final Supplier<DataComponentType<Integer>> CURRENCY_VALUE =
            DATA_COMPONENTS.registerComponentType("currency_value", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /** 古代神秘物品变体（「有少许神秘力量的古代物品」系列实例标识）。 */
    public static final Supplier<DataComponentType<AncientArtifactData>> ANCIENT_ARTIFACT =
            DATA_COMPONENTS.registerComponentType("ancient_artifact", builder -> builder
                    .persistent(AncientArtifactData.CODEC)
                    .networkSynchronized(AncientArtifactData.STREAM_CODEC));
}
