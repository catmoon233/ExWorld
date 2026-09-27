package com.wan.gmmod.content.abilities;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.content.charm.CharmManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 「误导」——教唆者（魔女途径 · 序列 8）主动，两段式目标选择。
 * <p>
 * 第一次触发：选定 15 米内被误导的生物（记入 {@code MISDIRECT_SOURCE}）；
 * 第二次触发：选定受害目标，被误导生物持续攻击它 15 秒。
 * 灵性（10）与冷却（45 秒）在第二段成功后才结算，
 * 因此构造器中消耗 / 冷却填 0，由本类手动写入 {@code SKILL_COOLDOWNS}。
 */
public class MisdirectAbility extends Ability {
    private static final double RANGE = 15.0;
    private static final int DURATION = 15 * 20;
    private static final int COST = 10;
    private static final int COOLDOWN = 45 * 20;

    public MisdirectAbility() {
        super(GuimiMod.id("misdirect"), 0, 0, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        String sourceUuid = sp.getData(ModAttachments.MISDIRECT_SOURCE);
        LivingEntity picked = AbilityTargeting.pickLivingEntity(sp, RANGE);
        if (sourceUuid.isEmpty()) {
            // 第一段：选定被误导的生物
            if (!(picked instanceof Mob)) {
                sp.displayClientMessage(Component.translatable("message.guimi_mod.misdirect.no_target"), true);
                return;
            }
            sp.setData(ModAttachments.MISDIRECT_SOURCE, picked.getUUID().toString());
            sp.displayClientMessage(Component.translatable("message.guimi_mod.misdirect.picked",
                    picked.getDisplayName()), true);
            return;
        }
        // 第二段：选定受害目标
        sp.setData(ModAttachments.MISDIRECT_SOURCE, "");
        if (!(level.getEntity(UUID.fromString(sourceUuid)) instanceof Mob source) || !source.isAlive()) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.misdirect.source_lost"), true);
            return;
        }
        if (picked == null || picked == source) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.misdirect.no_victim"), true);
            return;
        }
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - COST);
        CharmManager.misdirect(source, sp, picked, DURATION);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.misdirect.done",
                source.getDisplayName(), picked.getDisplayName()), true);
        // 手动记录冷却（第二段成功后才进入冷却）
        long now = level.getGameTime();
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(getId(), now + COOLDOWN, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
