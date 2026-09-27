package com.wan.gmmod.content.war;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityTargeting;
import com.wan.gmmod.content.charm.CharmManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * 「挑衅」——挑衅者（战争之红途径 · 序列 8）主动。
 * <p>
 * 对 15 米内目标使用，按玩家对目标的了解程度结算：
 * <ul>
 *   <li>无了解：激怒 5 秒；</li>
 *   <li>掌握情报（弱点洞察分析过该类型）：激怒 20 秒 + 攻击失准加剧；</li>
 * </ul>
 * 激怒 = 强制锁定挑衅者（复用 CharmManager 误导，受害者为挑衅者自身）+
 * 移速 +20% + 命中率 -30%（失准判定在 WarAbilityEventSubscriber）。
 * 对无智能生物同样散发憎恶气味，使其优先攻击挑衅者。冷却 20 秒（选中目标才结算）。
 */
public class ProvokeAbility extends Ability {
    private static final double RANGE = 15.0;
    private static final int COST = 5;
    private static final int COOLDOWN = 20 * 20;

    public ProvokeAbility() {
        super(GuimiMod.id("provoke"), 0, 0, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity picked = AbilityTargeting.pickLivingEntity(sp, RANGE);
        if (!(picked instanceof Mob mob)) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.provoke.no_target"), true);
            return;
        }
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - COST);

        boolean known = InsightManager.isKnown(sp, mob);
        int duration = (known ? 20 : 5) * 20;
        // 激怒：锁定挑衅者为攻击目标 + 移速 +20%；掌握情报时失准加剧（放大等级 1）
        CharmManager.misdirect(mob, sp, sp, duration);
        mob.addEffect(new MobEffectInstance(ModEffects.ENRAGED, duration, known ? 1 : 0));
        level.playSound(null, mob.blockPosition(),
                SoundEvents.GOAT_SCREAMING_AMBIENT, SoundSource.PLAYERS, 0.8F, 1.2F);
        sp.displayClientMessage(Component.translatable(
                known ? "message.guimi_mod.provoke.done_known" : "message.guimi_mod.provoke.done",
                mob.getDisplayName()), true);

        // 手动记录冷却（选中目标成功后才进入冷却）
        long now = level.getGameTime();
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(getId(), now + COOLDOWN, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
