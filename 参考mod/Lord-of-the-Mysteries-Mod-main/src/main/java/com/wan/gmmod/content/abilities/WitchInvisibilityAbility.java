package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 「隐形」——女巫（魔女途径 · 序列 7）主动。
 * <p>
 * 触发后做「抬手抹脸」动作（挥手），身体在 2 秒过渡中逐渐消失，
 * 随后获得完全隐身 30 秒。攻击或受到伤害立即中断。冷却 90 秒，消耗 20 灵性。
 * 过渡与隐身的维持由 {@code WitchPathwayManager#tickPlayer} 驱动，
 * 中断逻辑见 {@code WitchAbilityEventSubscriber}。
 */
public class WitchInvisibilityAbility extends Ability {
    /** 过渡时长（刻，2 秒） */
    public static final int TRANSITION_TICKS = 40;
    /** 完全隐身时长（刻，30 秒） */
    public static final int DURATION_TICKS = 30 * 20;

    public WitchInvisibilityAbility() {
        super(GuimiMod.id("witch_invisibility"), 20, 90 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        // 抬手抹脸动作：挥动主手
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        player.setData(ModAttachments.WITCH_INVIS_START, player.level().getGameTime());
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.3F);
        player.displayClientMessage(Component.translatable("message.guimi_mod.witch_invisibility.start"), true);
    }

    @Override
    public void onDeactivate(Player player) {
        player.setData(ModAttachments.WITCH_INVIS_START, 0L);
        player.setData(ModAttachments.WITCH_INVIS_END, 0L);
    }
}
