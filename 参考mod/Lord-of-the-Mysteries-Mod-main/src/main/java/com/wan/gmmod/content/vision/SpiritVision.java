package com.wan.gmmod.content.vision;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Spirit vision is a sequence 9 toggle. Higher ranks do not keep it. */
public final class SpiritVision {
    public static final int SEQUENCE = 9;

    private SpiritVision() {}

    public static boolean allowed(Player player) {
        return player != null
                && player.getData(ModAttachments.SEQUENCE_LEVEL) == SEQUENCE
                && Sequences.employed(player.getData(ModAttachments.PATHWAY));
    }

    public static void turnOff(Player player) {
        if (player == null) return;
        if (Boolean.TRUE.equals(player.getData(ModAttachments.SPIRIT_VISION))) {
            player.setData(ModAttachments.SPIRIT_VISION, false);
        }
        player.removeEffect(MobEffects.NIGHT_VISION);
    }
}