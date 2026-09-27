package com.wan.gmmod.common.item;

import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.content.characteristics.CharacteristicData;
import com.wan.gmmod.content.characteristics.MagicArtifactData;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 非凡特性物品：特性的物理形态。
 * <p>
 * 所有途径、所有等级的特性共用这一种物品，靠 {@link ModDataComponents#CHARACTERISTIC} 组件区分。
 * 通过 {@link #create(Sequences.Pathway, int)} 生成携带指定途径 / 等级的特性物品。
 * 本物品设为防火（{@code fireResistant}），配合事件监听实现「不灭定律」——特性不会被普通手段摧毁。
 */
public class CharacteristicItem extends Item {
    public CharacteristicItem(Properties properties) {
        super(properties.stacksTo(64).fireResistant());
    }

    /** 生成一份携带指定途径 / 等级的特性物品。 */
    public static ItemStack create(Sequences.Pathway pathway, int level) {
        return create(pathway, level, 1);
    }

    /** 生成指定数量、携带指定途径 / 等级的特性物品。 */
    public static ItemStack create(Sequences.Pathway pathway, int level, int count) {
        ItemStack stack = new ItemStack(
                com.wan.gmmod.common.registry.ModItems.CHARACTERISTIC.get(), count);
        stack.set(ModDataComponents.CHARACTERISTIC.get(), new CharacteristicData(pathway.getKey(), level));
        applyDoorSequenceModel(stack, pathway, level);
        return stack;
    }

    /** 门途径序列物品按序列号切换到专属图标（序列 9~1 各一张，经 custom_model_data 覆盖）。 */
    private static void applyDoorSequenceModel(ItemStack stack, Sequences.Pathway pathway, int level) {
        if (pathway == Sequences.Pathway.DOOR && level >= 1 && level <= 9) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(level));
        }
    }

    /** 读取物品上的特性数据，非特性物品或无数据返回 {@code null}。 */
    public static CharacteristicData getData(ItemStack stack) {
        return stack.get(ModDataComponents.CHARACTERISTIC.get());
    }

    /**
     * 主动植入：手持门途径（{@code door}）非凡特性、另一只手持有耐久装备时右键，
     * 消耗特性，把能力「附魔」到装备上——类似附魔，装备本体保留，特性能力附加其上。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        CharacteristicData data = getData(held);
        if (data == null || !"door".equals(data.pathway())) {
            return InteractionResultHolder.pass(held);
        }
        InteractionHand other = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack base = player.getItemInHand(other);
        if (!MagicArtifactItem.isImplantableBase(base)) {
            player.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.implant_no_base"), true);
            return InteractionResultHolder.pass(held);
        }
        if (!level.isClientSide) {
            held.shrink(1);
            // 保留基底物品，把特性能力附加到基底上（类似附魔）
            base.set(ModDataComponents.MAGIC_ARTIFACT.get(), new MagicArtifactData(
                    data.pathway(), data.level(),
                    level.random.nextInt(4),
                    BuiltInRegistries.ITEM.getKey(base.getItem()).toString()));
            player.displayClientMessage(Component.translatable(
                    "message.guimi_mod.magic.implanted"), true);
        }
        return InteractionResultHolder.success(held);
    }

    @Override
    public Component getName(ItemStack stack) {
        CharacteristicData data = getData(stack);
        if (data == null) {
            return super.getName(stack);
        }
        Sequences.Pathway pathway = Sequences.fromKey(data.pathway());
        String pathwayName = pathway == null ? data.pathway() : pathway.getDisplayName();
        String seqName = pathway == null ? ("序列" + data.level()) : pathway.getSequenceName(data.level());
        return Component.translatable("item.guimi_mod.characteristic.named", pathwayName, seqName)
                .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CharacteristicData data = getData(stack);
        if (data != null) {
            tooltip.add(Component.translatable("item.guimi_mod.characteristic.desc", data.level())
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
