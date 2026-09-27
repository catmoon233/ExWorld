package com.wan.gmmod.common.item;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.gui.MirrorDivinationScreenOpener;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 镜子物品——女巫 / 欢愉魔女专用魔法道具。
 * <ul>
 *   <li><b>潜行右键</b>：消耗灵性在当前位置绑定「镜子替身」锚点
 *   （写入 {@link ModAttachments#MIRROR_ANCHOR}），需已解锁镜子替身能力。</li>
 *   <li><b>普通右键</b>：已解锁「魔镜占卜」时，打开占卜界面（客户端），
 *   界面选择「占卜 / 反占卜 / 通灵」三种模式后由
 *   {@code MirrorDivinationPacket} 在服务端执行。</li>
 * </ul>
 */
public class MirrorItem extends Item {
    /** 绑定替身锚点消耗的灵性 */
    private static final int BIND_COST = 20;

    public MirrorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            // 潜行右键：绑定镜子替身锚点
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                bindAnchor(sp);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // 普通右键：打开魔镜占卜界面
        if (!SkillManager.isUnlocked(player, GuimiMod.id("mirror_divination"))) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            MirrorDivinationScreenOpener.open();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 服务端：绑定镜子替身锚点。需已解锁镜子替身能力且灵性充足。 */
    private static void bindAnchor(ServerPlayer sp) {
        if (!SkillManager.isUnlocked(sp, GuimiMod.id("mirror_substitute"))) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.mirror.locked"), true);
            return;
        }
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < BIND_COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - BIND_COST);
        BlockPos pos = sp.blockPosition();
        String anchor = sp.level().dimension().location() + ";"
                + pos.getX() + ";" + pos.getY() + ";" + pos.getZ();
        sp.setData(ModAttachments.MIRROR_ANCHOR, anchor);
        if (sp.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    sp.getX(), sp.getY() + 1.0, sp.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.3F);
        }
        sp.displayClientMessage(Component.translatable("message.guimi_mod.mirror.bound"), true);
    }
}
