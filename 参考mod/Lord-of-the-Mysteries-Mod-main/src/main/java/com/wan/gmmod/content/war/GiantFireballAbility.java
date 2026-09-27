package com.wan.gmmod.content.war;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.entities.FlameOrbEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 「巨大火球」——纵火家（战争之红途径 · 序列 7）主动，凝聚 + 右键释放。
 * <p>
 * 触发能力后巨大火球凝聚握在主手（凝聚纹理显示在手臂上），蓄力 15~20 秒
 * （炽白压缩强化后 7.5~10 秒）内右键掷出超大赤红火球：伤害 18，范围 6 米，
 * 爆炸冲击波击退。蓄力不足提示继续凝聚，超过窗口上限则凝聚散逸
 * （由 {@link WarPathwayManager} 每刻检查）。冷却 60 秒。
 */
public class GiantFireballAbility extends Ability {
    /** 蓄力下限 15 秒 / 上限 20 秒；炽白压缩强化 -50%。 */
    private static final int MIN_CHARGE_TICKS = 15 * 20;
    private static final int MAX_CHARGE_TICKS = 20 * 20;
    private static final int COST = 20;
    private static final int COOLDOWN = 60 * 20;

    public GiantFireballAbility() {
        super(GuimiMod.id("giant_fireball"), 0, 0, true);
    }

    /** 蓄力达标下限（炽白压缩强化后减半）。 */
    public static int minChargeTicks(ServerPlayer sp) {
        return SkillManager.isUnlocked(sp, GuimiMod.id("white_compression"))
                ? MIN_CHARGE_TICKS / 2 : MIN_CHARGE_TICKS;
    }

    /** 蓄力窗口上限（炽白压缩强化后减半），超窗凝聚散逸。 */
    public static int maxChargeTicks(ServerPlayer sp) {
        return SkillManager.isUnlocked(sp, GuimiMod.id("white_compression"))
                ? MAX_CHARGE_TICKS / 2 : MAX_CHARGE_TICKS;
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        long start = sp.getData(ModAttachments.GIANT_FIREBALL_CHARGE_START);
        if (start == 0L || now - start > maxChargeTicks(sp)) {
            // 开始（或超窗后重新开始）凝聚：巨大火球握入主手，蓄力达标后右键掷出
            sp.setData(ModAttachments.GIANT_FIREBALL_CHARGE_START, now);
            WarPathwayManager.giveChargedOrb(sp, true);
            sp.displayClientMessage(Component.translatable("message.guimi_mod.giant_fireball.charging"), true);
            return;
        }
        // 再次按技能键：与右键等价，直接释放
        release(sp);
    }

    /** 释放巨大凝聚火球（右键凝聚火球物品 / 二次触发技能键时调用，服务端）。 */
    public static void release(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        long start = sp.getData(ModAttachments.GIANT_FIREBALL_CHARGE_START);
        if (start == 0L) {
            return;
        }
        if (now - start < minChargeTicks(sp)) {
            // 蓄力未满：提示剩余秒数，继续凝聚
            int remain = (int) Math.ceil((minChargeTicks(sp) - (now - start)) / 20.0);
            sp.displayClientMessage(Component.translatable(
                    "message.guimi_mod.giant_fireball.not_ready", remain), true);
            return;
        }
        sp.setData(ModAttachments.GIANT_FIREBALL_CHARGE_START, 0L);
        WarPathwayManager.removeChargedOrbs(sp, true);
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - COST);

        FlameOrbEntity orb = new FlameOrbEntity(level, sp, 18.0F, 6.0F, false, true);
        orb.setPos(sp.getX(), sp.getEyeY() - 0.2, sp.getZ());
        orb.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 1.2F, 0.5F);
        level.addFreshEntity(orb);
        level.playSound(null, sp.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.5F);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.giant_fireball.done"), true);

        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(GuimiMod.id("giant_fireball"), now + COOLDOWN, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
