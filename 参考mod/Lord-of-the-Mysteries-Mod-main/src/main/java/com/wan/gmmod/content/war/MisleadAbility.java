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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * 「误导」——阴谋家（战争之红途径 · 序列 6）主动，两段式目标选择。
 * <p>
 * 第一次触发：选定 20 米内被误导的生物（记入 {@code MISLEAD_SOURCE}）；
 * 第二次触发：选定「错误判断」的对象，被误导生物将其视作敌人攻击 30 秒。
 * 灵性（10）与冷却（45 秒）在第二段成功后才结算，构造器填 0 手动写入。
 */
public class MisleadAbility extends Ability {
    private static final double RANGE = 20.0;
    private static final int DURATION = 30 * 20;
    private static final int COST = 10;
    private static final int COOLDOWN = 45 * 20;

    public MisleadAbility() {
        super(GuimiMod.id("mislead"), 0, 0, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        String sourceUuid = sp.getData(ModAttachments.MISLEAD_SOURCE);
        LivingEntity picked = AbilityTargeting.pickLivingEntity(sp, RANGE);
        if (sourceUuid.isEmpty()) {
            // 第一段：选定被误导的生物
            if (!(picked instanceof Mob)) {
                sp.displayClientMessage(Component.translatable("message.guimi_mod.mislead.no_target"), true);
                return;
            }
            sp.setData(ModAttachments.MISLEAD_SOURCE, picked.getUUID().toString());
            sp.displayClientMessage(Component.translatable("message.guimi_mod.mislead.picked",
                    picked.getDisplayName()), true);
            return;
        }
        // 第二段：选定被「误认为敌人」的对象
        sp.setData(ModAttachments.MISLEAD_SOURCE, "");
        if (!(level.getEntity(UUID.fromString(sourceUuid)) instanceof Mob source) || !source.isAlive()) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.mislead.source_lost"), true);
            return;
        }
        if (picked == null || picked == source) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.mislead.no_victim"), true);
            return;
        }
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - COST);
        CharmManager.misdirect(source, sp, picked, DURATION);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.mislead.done",
                source.getDisplayName(), picked.getDisplayName()), true);
        // 手动记录冷却（第二段成功后才进入冷却）
        long now = level.getGameTime();
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(getId(), now + COOLDOWN, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
