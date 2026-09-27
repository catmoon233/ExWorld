package com.wan.gmmod.content.witch;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.sequences.Sequence;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 晋升 / 降级钩子。
 * <p>
 * 目前用于女巫「性别转换」：男性玩家晋升为女巫（魔女途径 · 序列 7 及更强）时
 * 自动切换为女性形态（写入 {@link ModAttachments#FEMALE_FORM}），永久生效；
 * 切换到其他途径或降级到序列 7 以下时解除。
 * 已安装 Female Gender Mod 时由 {@link FemaleGenderCompat} 同步其性别设置。
 */
public final class PromotionHooks {
    private PromotionHooks() {}

    /** 女巫「性别转换」生效的序列阈值（序列号 ≤ 7，即达到「女巫」或更强）。 */
    private static final int WITCH_GENDER_LEVEL = 7;

    /**
     * 晋升 / 就职成功后回调（服务端）。
     *
     * @param player    晋升的玩家
     * @param targetSeq 晋升到的目标序列
     */
    public static void onPromoted(Player player, Sequence targetSeq) {
        if (!(player instanceof ServerPlayer sp) || targetSeq == null) {
            return;
        }
        boolean becomeWitchFemale = targetSeq.getPathway() == Sequences.Pathway.WITCH
                && targetSeq.getLevel() <= WITCH_GENDER_LEVEL;
        boolean currentlyFemale = sp.getData(ModAttachments.FEMALE_FORM);

        if (becomeWitchFemale && !currentlyFemale) {
            sp.setData(ModAttachments.FEMALE_FORM, true);
            FemaleGenderCompat.setFemale(sp, true);
            sp.displayClientMessage(Component.translatable("message.guimi_mod.gender_transition"), false);
        } else if (!becomeWitchFemale && currentlyFemale) {
            // 切换途径或降级到序列 7 以下：解除女性形态
            sp.setData(ModAttachments.FEMALE_FORM, false);
            FemaleGenderCompat.setFemale(sp, false);
        }
    }
}
