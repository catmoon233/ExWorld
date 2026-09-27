package com.wan.gmmod.common.item;

import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.characteristics.CharacteristicData;
import com.wan.gmmod.content.characteristics.SealedArtifactData;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 封印物物品：将非凡特性「封印」进装备后得到的强力物品，同时附带代价。
 * <p>
 * 所有途径 / 等级 / 基底物品共用这一种物品，靠 {@link ModDataComponents#SEALED_ARTIFACT}
 * 组件区分。由工匠（完美者序列6 机械专家及以上）的封印技能
 * （{@code par_seal_artifact}）或非凡生物死亡时与附近物品融合获得。
 * <p>
 * 持有 / 穿戴期间，持续获得途径对应的正面效果，同时承受「封印侵蚀」的代价，
 * 效果强度随封印特性的序列等级缩放（序列 0 最强、序列 9 最弱）。见
 * {@link com.wan.gmmod.common.event.SealedArtifactEventSubscriber}。
 */
public class SealedArtifactItem extends Item {
    public SealedArtifactItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    /** 将指定基底物品与一份非凡特性封印成封印物物品。 */
    public static ItemStack create(ItemStack base, CharacteristicData data) {
        ItemStack stack = new ItemStack(ModItems.SEALED_ARTIFACT.get());
        stack.set(ModDataComponents.SEALED_ARTIFACT.get(), new SealedArtifactData(
                data.pathway(), data.level(),
                BuiltInRegistries.ITEM.getKey(base.getItem()).toString()));
        return stack;
    }

    /** 读取封印物上的数据，非封印物或无数据返回 {@code null}。 */
    public static SealedArtifactData getData(ItemStack stack) {
        return stack.get(ModDataComponents.SEALED_ARTIFACT.get());
    }

    /** 是否是一份「可被封印」的非凡特性物品。 */
    public static boolean isCharacteristic(ItemStack stack) {
        return CharacteristicItem.getData(stack) != null;
    }

    /** 是否可作为封印基底：耐久装备（工具 / 武器 / 盔甲），且本身不是特性或封印物。 */
    public static boolean isSealableBase(ItemStack stack) {
        if (stack.isEmpty() || isCharacteristic(stack) || getData(stack) != null) {
            return false;
        }
        return stack.isDamageableItem();
    }

    /** 序列等级对应的力量档位：0 = 最强（序列 0），9 = 最弱（序列 9）。 */
    public static int tier(int level) {
        return Sequences.MAX_LEVEL - level;
    }

    /** 正面效果等级（效果放大值，0 = 效果 I）：序列等级越低（越强）增幅越大。 */
    public static int positiveAmplifier(int level) {
        return Math.min(4, Math.max(0, (tier(level) + 1) / 2));
    }

    /** 负面侵蚀等级（效果放大值，0 = 效果 I）：与正面效果同步上升，力量越大代价越重。
     *  作为可有效利用的非凡物品，封印物的负面较直接携带特性更温和（tier/3，上限 III）。 */
    public static int negativeAmplifier(int level) {
        return Math.min(3, Math.max(0, tier(level) / 3));
    }

    /** 途径对应的正面效果：封印该途径特性时赋予持有者的增益。 */
    public static Holder<MobEffect> positiveEffect(Sequences.Pathway pathway) {
        return switch (pathway) {
            case FOOL, DARKNESS -> MobEffects.NIGHT_VISION;        // 洞察灵视
            case ERROR, HERMIT, BLACK_EMPEROR, WHEEL -> MobEffects.LUCK; // 命运眷顾
            case DOOR, WITCH -> MobEffects.MOVEMENT_SPEED;         // 迅捷
            case PARAGON -> MobEffects.DIG_SPEED;                  // 工匠效率
            case HANGED_MAN, JUSTICE -> MobEffects.ABSORPTION;     // 圣堂庇护
            case SUN, MOON, MOTHER -> MobEffects.REGENERATION;     // 神光 / 月华治愈
            case TYRANT, WAR, ABYSS -> MobEffects.DAMAGE_BOOST;    // 巨力
            case WHITE_TOWER, GIANT, CHAINED -> MobEffects.DAMAGE_RESISTANCE; // 坚韧体魄
            case VISIONARY -> MobEffects.INVISIBILITY;             // 梦境隐匿
            case DEATH -> MobEffects.FIRE_RESISTANCE;              // 冥火不焚
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        SealedArtifactData data = getData(stack);
        if (data == null) {
            return super.getName(stack);
        }
        Sequences.Pathway pathway = Sequences.fromKey(data.pathway());
        String pathwayName = pathway == null ? data.pathway() : pathway.getDisplayName();
        String seqName = pathway == null ? ("序列" + data.level()) : pathway.getSequenceName(data.level());
        return Component.translatable("item.guimi_mod.sealed_artifact.named", pathwayName, seqName)
                .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SealedArtifactData data = getData(stack);
        if (data != null) {
            Item base = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(data.baseItem()));
            tooltip.add(Component.translatable("item.guimi_mod.sealed_artifact.base",
                    base.getDefaultInstance().getHoverName()).withStyle(ChatFormatting.GRAY));

            Sequences.Pathway pathway = Sequences.fromKey(data.pathway());
            if (pathway != null) {
                int posAmp = positiveAmplifier(data.level());
                tooltip.add(Component.translatable("item.guimi_mod.sealed_artifact.positive",
                        effectName(positiveEffect(pathway), posAmp)).withStyle(ChatFormatting.GREEN));
                int negAmp = negativeAmplifier(data.level());
                if (negAmp >= 1) {
                    tooltip.add(Component.translatable("item.guimi_mod.sealed_artifact.negative",
                            effectName(com.wan.gmmod.common.registry.ModEffects.SEALED_CORRUPTION, negAmp))
                            .withStyle(ChatFormatting.DARK_RED));
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private static Component effectName(Holder<MobEffect> effect, int amplifier) {
        Component name = Component.translatable(effect.value().getDescriptionId());
        return amplifier >= 1
                ? Component.translatable("item.guimi_mod.sealed_artifact.effect_level", name, roman(amplifier + 1))
                : name;
    }

    /** 将 1 起的等级数字转为罗马数字（1 ~ 20）。 */
    public static String roman(int value) {
        if (value <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value; i++) {
            sb.append('I');
        }
        return sb.toString().replace("IIIII", "V")
                .replace("IIII", "IV")
                .replace("VV", "X")
                .replace("VIV", "IX");
    }
}
