package com.wan.gmmod.common.item;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModDataComponents;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.content.entities.TalismanProjectileEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 灵性符咒：由祭台在灵性之墙内合成，投掷后激发对应祈求对象的效果。
 * <p>
 * 使用流程（两段式）：
 * <ol>
 *   <li>右键开始念咒——消耗 1 点灵性，玩家站立不动持续 1 秒；</li>
 *   <li>念咒完成符咒「灌注」发光，3 秒内再次右键投掷；</li>
 *   <li>符咒飞行约 0.5 秒后在落点 / 命中目标处激发效果（见
 *       {@link TalismanProjectileEntity}）。</li>
 * </ol>
 * 念咒期间移动即被打断；超过 3 秒未投掷则灌注消散。
 */
public class TalismanItem extends Item {

    /** 念咒所需时长（刻） */
    private static final int CHANT_TICKS = 20;
    /** 灌注后允许投掷的窗口（刻）：3 秒 */
    private static final int CHARGE_WINDOW_TICKS = 60;
    /** 念咒激活费（灵性） */
    private static final int CHANT_SPIRIT_COST = 1;

    public TalismanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        ServerPlayer sp = (ServerPlayer) player;

        // 已灌注：3 秒窗口内右键投掷
        Long chargeEnd = stack.get(ModDataComponents.TALISMAN_CHARGE_END.get());
        if (chargeEnd != null && level.getGameTime() < chargeEnd) {
            throwTalisman(sp, stack);
            return InteractionResultHolder.success(stack);
        }

        // 正在念咒中
        long chantStart = sp.getData(ModAttachments.TALISMAN_CHANT_START);
        if (chantStart > 0) {
            sp.displayClientMessage(
                    Component.translatable("message.guimi_mod.talisman_chanting"), true);
            return InteractionResultHolder.success(stack);
        }

        // 开始念咒：校验并扣除灵性
        int spirit = SpiritualityManager.get(sp);
        if (spirit < CHANT_SPIRIT_COST) {
            sp.displayClientMessage(
                    Component.translatable("message.guimi_mod.talisman_no_spirit", CHANT_SPIRIT_COST), true);
            return InteractionResultHolder.success(stack);
        }
        SpiritualityManager.set(sp, spirit - CHANT_SPIRIT_COST);
        sp.setData(ModAttachments.TALISMAN_CHANT_START, level.getGameTime());
        level.playSound(null, sp.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.8F, 1.1F);
        sp.displayClientMessage(
                Component.translatable("message.guimi_mod.talisman_chant_start"), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) {
            return;
        }
        if (!(entity instanceof ServerPlayer sp)) {
            return;
        }
        long now = level.getGameTime();

        // 灌注过期：移除发光与标记
        Long chargeEnd = stack.get(ModDataComponents.TALISMAN_CHARGE_END.get());
        if (chargeEnd != null) {
            if (now >= chargeEnd) {
                stack.remove(ModDataComponents.TALISMAN_CHARGE_END.get());
                stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
                if (selected) {
                    sp.displayClientMessage(
                            Component.translatable("message.guimi_mod.talisman_charge_expired"), true);
                }
            }
        }

        // 念咒进度
        long chantStart = sp.getData(ModAttachments.TALISMAN_CHANT_START);
        if (chantStart <= 0) {
            return;
        }
        // 移动即打断
        if (sp.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            sp.setData(ModAttachments.TALISMAN_CHANT_START, 0L);
            sp.displayClientMessage(
                    Component.translatable("message.guimi_mod.talisman_interrupted"), true);
            return;
        }
        // 1 秒念咒完成 → 灌注
        if (now >= chantStart + CHANT_TICKS) {
            sp.setData(ModAttachments.TALISMAN_CHANT_START, 0L);
            stack.set(ModDataComponents.TALISMAN_CHARGE_END.get(), now + CHARGE_WINDOW_TICKS);
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            level.playSound(null, sp.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.7F, 1.4F);
            sp.displayClientMessage(
                    Component.translatable("message.guimi_mod.talisman_charged"), true);
        }
    }

    /** 投掷符咒：发射投射物并消耗一格物品。 */
    private void throwTalisman(ServerPlayer sp, ItemStack stack) {
        Level level = sp.level();
        TalismanProjectileEntity projectile = new TalismanProjectileEntity(level, sp);
        projectile.setItem(stack.copy());
        projectile.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 1.3F, 0.4F);
        level.addFreshEntity(projectile);
        stack.shrink(1);
        level.playSound(null, sp.blockPosition(), SoundEvents.SNOWBALL_THROW,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        sp.displayClientMessage(
                Component.translatable("message.guimi_mod.talisman_thrown"), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var data = stack.get(ModDataComponents.TALISMAN.get());
        if (data != null) {
            tooltip.add(Component.translatable("item.guimi_mod.talisman.deity",
                    Component.translatable("talisman.guimi_mod.deity." + data.deity())));
        }
        tooltip.add(Component.translatable("item.guimi_mod.talisman.tooltip"));
    }
}