package com.wan.gmmod.content.war;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityTargeting;
import com.wan.gmmod.content.charm.CharmManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 「煽动」——阴谋家（战争之红途径 · 序列 6）主动。
 * <p>
 * 选定 20 米内的视线目标后，玩家周围 10 米内的中立 / 被动生物被煽动，
 * 进入敌对状态攻击该目标，持续 20 秒。冷却 60 秒（煽动成功才结算）。
 */
public class InciteAbility extends Ability {
    private static final double TARGET_RANGE = 20.0;
    private static final double MOB_RANGE = 10.0;
    private static final int DURATION = 20 * 20;
    private static final int COST = 12;
    private static final int COOLDOWN = 60 * 20;

    public InciteAbility() {
        super(GuimiMod.id("incite"), 0, 0, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity victim = AbilityTargeting.pickLivingEntity(sp, TARGET_RANGE);
        if (victim == null) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.incite.no_target"), true);
            return;
        }
        // 周围 10 米内的中立 / 被动生物（排除天生敌对怪物与目标本身）
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class,
                sp.getBoundingBox().inflate(MOB_RANGE),
                m -> m.isAlive() && m != victim && !(m instanceof Enemy));
        if (mobs.isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.incite.no_mobs"), true);
            return;
        }
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - COST);
        for (Mob mob : mobs) {
            CharmManager.misdirect(mob, sp, victim, DURATION);
        }
        level.playSound(null, sp.blockPosition(),
                SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 0.8F, 1.4F);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.incite.done",
                mobs.size(), victim.getDisplayName()), true);

        long now = level.getGameTime();
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(getId(), now + COOLDOWN, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
