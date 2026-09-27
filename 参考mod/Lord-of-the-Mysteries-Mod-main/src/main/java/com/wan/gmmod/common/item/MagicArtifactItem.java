package com.wan.gmmod.common.item;

import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.characteristics.CharacteristicData;
import com.wan.gmmod.content.characteristics.MagicArtifactData;
import com.wan.gmmod.content.magic.MagicArtifactManager;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * 神奇物品（封印物）：将非凡特性「植入」基础物品后形成的超凡物品。
 * <p>
 * 拥有正面加强（提供超凡能力）、负面作用（使用代价 / 反噬），且效果具有随机性——
 * 生成时掷定一种随机性变体（{@link MagicArtifactData#variant()}），决定能力参数浮动。
 * 所有途径 / 等级 / 基底共用同一种物品，靠 {@link ModDataComponents#MAGIC_ARTIFACT}
 * 组件区分。
 * <p>
 * 目前完整实装门途径（{@code door}）序列 9 ~ 5（学徒 / 戏法大师 / 占星人 / 记录官 / 旅行家）：
 * 右键触发各自能力（传送 / 空间折叠 / 星象解读 / 记录使用 / 传送门），持续负面由
 * {@link com.wan.gmmod.common.event.MagicArtifactEventSubscriber} 处理。
 */
public class MagicArtifactItem extends Item {
    public MagicArtifactItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    /** 将指定基底物品与一份非凡特性植入成神奇物品，随机掷定变体。 */
    public static ItemStack create(ItemStack base, CharacteristicData data, Level level) {
        ItemStack stack = new ItemStack(ModItems.MAGIC_ARTIFACT.get());
        stack.set(ModDataComponents.MAGIC_ARTIFACT.get(), new MagicArtifactData(
                data.pathway(), data.level(),
                level.random.nextInt(4), // 0=标准，1~3=随机性变体
                BuiltInRegistries.ITEM.getKey(base.getItem()).toString()));
        // 门途径按序列显示专属图标（序列 9~1 各一张，经 custom_model_data 覆盖）
        Sequences.Pathway pathway = Sequences.fromKey(data.pathway());
        if (pathway == Sequences.Pathway.DOOR && data.level() >= 1 && data.level() <= 9) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(data.level()));
        }
        return stack;
    }

    /** 读取神奇物品上的数据，非神奇物品或无数据返回 {@code null}。 */
    public static MagicArtifactData getData(ItemStack stack) {
        return stack.get(ModDataComponents.MAGIC_ARTIFACT.get());
    }

    /** 是否是一份「可被植入」的非凡特性物品。 */
    public static boolean isCharacteristic(ItemStack stack) {
        return CharacteristicItem.getData(stack) != null;
    }

    /** 是否可作为植入基底：耐久装备（工具 / 武器 / 盔甲），且本身不是特性、封印物或神奇物品。 */
    public static boolean isImplantableBase(ItemStack stack) {
        if (stack.isEmpty() || isCharacteristic(stack)
                || SealedArtifactItem.getData(stack) != null || getData(stack) != null) {
            return false;
        }
        return stack.isDamageableItem();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && MagicArtifactManager.onUse(player, player.getItemInHand(hand))) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public Component getName(ItemStack stack) {
        MagicArtifactData data = getData(stack);
        if (data == null) {
            return super.getName(stack);
        }
        Sequences.Pathway pathway = Sequences.fromKey(data.pathway());
        String pathwayName = pathway == null ? data.pathway() : pathway.getDisplayName();
        String seqName = pathway == null ? ("序列" + data.level()) : pathway.getSequenceName(data.level());
        return Component.translatable("item.guimi_mod.magic_artifact.named", pathwayName, seqName)
                .withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        appendMagicTooltip(stack, tooltip);
        super.appendHoverText(stack, context, tooltip, flag);
    }

    /** 为任意携带 {@link ModDataComponents#MAGIC_ARTIFACT} 组件的物品追加能力说明（神奇物品通用 tooltip）。 */
    public static void appendMagicTooltip(ItemStack stack, List<Component> tooltip) {
        MagicArtifactData data = getData(stack);
        if (data == null) {
            return;
        }
        Item base = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(data.baseItem()));
        tooltip.add(Component.translatable("item.guimi_mod.magic_artifact.base",
                base.getDefaultInstance().getHoverName()).withStyle(ChatFormatting.GRAY));

        Sequences.Pathway pathway = Sequences.fromKey(data.pathway());
        if (pathway != null) {
            tooltip.add(Component.translatable(
                            MagicArtifactManager.positiveKey(pathway, data.level()))
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable(
                            MagicArtifactManager.negativeKey(pathway, data.level()))
                    .withStyle(ChatFormatting.DARK_RED));
            if (data.variant() >= 1 && data.variant() <= 3) {
                tooltip.add(Component.translatable(
                                MagicArtifactManager.variantKey(pathway, data.level(), data.variant()))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }
}