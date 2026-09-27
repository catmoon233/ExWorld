package com.wan.gmmod.content.spirituality;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.SkillManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/**
 * Spirituality is Iron's Spells mana. Sequence rank only raises the mana cap.
 */
public final class SpiritualityManager {
    public static final int INFINITE = Integer.MAX_VALUE;
    private static final int IRON_BASE = 100;
    private static final int SEQUENCE_ZERO_BONUS = 10000;
    private static final ResourceLocation CAP = ResourceLocation.fromNamespaceAndPath(GuimiMod.MODID, "sequence_mana");
    private static final int[] BASE_BY_LEVEL = {
            INFINITE, 2000, 1000, 600, 500, 300, 200, 150, 120, 100
    };
    private static final int TRAINING_CYCLES_PER_POINT = 30;
    private static final int TRAINING_BONUS_CAP = 50;

    private SpiritualityManager() {}

    public static int get(Player player) {
        if (player == null) return 0;
        return Math.max(0, Math.round(MagicData.getPlayerMagicData(player).getMana()));
    }

    public static void set(Player player, int value) {
        if (player == null) return;
        MagicData.getPlayerMagicData(player).setMana(Math.max(0, value));
    }

    public static int getMax(Player player) {
        if (player == null) return 0;
        AttributeInstance mana = player.getAttribute(AttributeRegistry.MAX_MANA);
        if (mana == null) return desired(player);
        return Math.max(0, (int) mana.getValue());
    }

    public static boolean isInfinite(int max) {
        return max >= SEQUENCE_ZERO_BONUS;
    }

    public static void applyCap(Player player) {
        if (player == null) return;
        AttributeInstance mana = player.getAttribute(AttributeRegistry.MAX_MANA);
        if (mana == null) return;
        mana.removeModifier(CAP);
        if (!com.wan.gmmod.content.sequences.Sequences.employed(player.getData(ModAttachments.PATHWAY))) return;
        int extra = bonus(desired(player));
        if (extra <= 0) return;
        mana.addOrUpdateTransientModifier(new AttributeModifier(CAP, extra, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void addTrainingCycle(Player player) {
        int cycles = player.getData(ModAttachments.MEDITATION_TRAINING);
        if (cycles < TRAINING_BONUS_CAP * TRAINING_CYCLES_PER_POINT) {
            player.setData(ModAttachments.MEDITATION_TRAINING, cycles + 1);
        }
    }

    private static int desired(Player player) {
        int level = player.getData(ModAttachments.SEQUENCE_LEVEL);
        if (!com.wan.gmmod.content.sequences.Sequences.employed(player.getData(ModAttachments.PATHWAY))) {
            return ModAttachments.DEFAULT_SPIRITUALITY;
        }
        if (level <= 0) return INFINITE;
        int base = BASE_BY_LEVEL[Math.min(level, BASE_BY_LEVEL.length - 1)];
        if (base == INFINITE) return INFINITE;
        return base + pathwayBonus(player) + meditationBonus(player);
    }

    private static int bonus(int desired) {
        if (desired == INFINITE) return SEQUENCE_ZERO_BONUS;
        return Math.max(0, desired - IRON_BASE);
    }

    private static int pathwayBonus(Player player) {
        int bonus = 0;
        for (Ability ability : SkillManager.getUnlockedAbilities(player)) {
            String path = ability.getId().getPath();
            if ("her_spirit_expand".equals(path)) bonus += 20;
            else if ("door_spirit_boost".equals(path)) bonus += 30;
        }
        return bonus;
    }

    private static int meditationBonus(Player player) {
        int cycles = player.getData(ModAttachments.MEDITATION_TRAINING);
        return Math.min(TRAINING_BONUS_CAP, cycles / TRAINING_CYCLES_PER_POINT);
    }
}