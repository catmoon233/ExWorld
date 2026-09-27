package com.wan.gmmod.content.ritual;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/**
 * 「美人鱼的歌声」环境 Buff 管理器。
 * <p>
 * 歌声 Buff（{@code guimi_mod:mermaid_song}）以隐藏形式施加（ambient、不可见、无图标），
 * 是「秘偶大师」晋升仪式的检测标记。Buff 分真假两种，外观完全相同：
 * <ul>
 *     <li>真 Buff：由野生美人鱼实体（或激活的遗迹）施加，或由「亲耳听过真歌声」的
 *     玩家使用高序列替代能力模拟；</li>
 *     <li>假 Buff：未亲历过真歌声的玩家使用替代能力时产生——服用魔药仍会失控。</li>
 * </ul>
 * 真伪只写在服务端附件 {@code MERMAID_SONG_GENUINE} 中，绝不同步、不提示玩家。
 */
public final class MermaidSongManager {

    /** 每次施加的 Buff 持续时间（刻），美人鱼会周期性刷新 */
    public static final int SONG_BUFF_DURATION = 600;
    /** 写入「亲耳听过真歌声」标记所需的累计暴露时间（30 秒） */
    public static final int HEARD_THRESHOLD_TICKS = 600;

    private MermaidSongManager() {}

    /**
     * 真歌声：由野生美人鱼周期性调用。刷新真 Buff，并累计暴露时间，
     * 停留满 30 秒后永久写入亲历标记（经历一次即可）。
     */
    public static void applyRealSong(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        addHiddenBuff(player);
        player.setData(ModAttachments.MERMAID_SONG_GENUINE, true);

        if (!player.getData(ModAttachments.HAS_HEARD_MERMAID_SONG)) {
            int exposure = player.getData(ModAttachments.MERMAID_SONG_EXPOSURE_TICKS);
            if (exposure >= HEARD_THRESHOLD_TICKS) {
                player.setData(ModAttachments.HAS_HEARD_MERMAID_SONG, true);
                player.displayClientMessage(
                        Component.translatable("message.guimi_mod.mermaid_song_heard"), false);
            }
        }
    }

    /** 真歌声范围内的暴露计时（美人鱼每刻调用），用于亲历标记判定。 */
    public static void tickExposure(Player player) {
        if (player.level().isClientSide || player.getData(ModAttachments.HAS_HEARD_MERMAID_SONG)) {
            return;
        }
        player.setData(ModAttachments.MERMAID_SONG_EXPOSURE_TICKS,
                player.getData(ModAttachments.MERMAID_SONG_EXPOSURE_TICKS) + 1);
    }

    /**
     * 替代能力模拟的歌声（供水手途径「深海歌谣」、月亮途径「月光圣水」、
     * 空想家途径「织梦人」等高序列能力调用）。
     * <p>
     * 只有亲耳听过真歌声的玩家才能正确模仿——否则施加的是假 Buff，
     * 外观完全相同，但服用魔药仍会失控。此判定悄悄进行，不告知玩家。
     *
     * @param source   施法者（决定真伪）
     * @param receiver 受益者
     * @param duration Buff 持续时间（刻）
     */
    public static void applyEquivalentSong(Player source, Player receiver, int duration) {
        if (receiver.level().isClientSide) {
            return;
        }
        receiver.addEffect(new MobEffectInstance(ModEffects.MERMAID_SONG, duration, 0,
                true, false, false));
        boolean genuine = source.getData(ModAttachments.HAS_HEARD_MERMAID_SONG);
        receiver.setData(ModAttachments.MERMAID_SONG_GENUINE, genuine);
    }

    /** 仪式条件是否满足：拥有歌声 Buff 且 Buff 为真（服务端悄悄判定）。 */
    public static boolean isRitualConditionMet(Player player) {
        return player.hasEffect(ModEffects.MERMAID_SONG)
                && player.getData(ModAttachments.MERMAID_SONG_GENUINE);
    }

    /** 以隐藏形式（ambient、不可见、无图标）刷新歌声 Buff。 */
    private static void addHiddenBuff(Player player) {
        player.addEffect(new MobEffectInstance(ModEffects.MERMAID_SONG, SONG_BUFF_DURATION, 0,
                true, false, false));
    }
}
