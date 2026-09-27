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
 * 「火球术」——纵火家（战争之红途径 · 序列 7）主动，凝聚 + 右键释放。
 * <p>
 * 触发能力后火焰凝聚成火球握在主手（凝聚纹理显示在手臂上），右键掷出：
 * <ul>
 *   <li>短蓄（未蓄满）：赤红火球，伤害 5，范围 2 米，冷却 3 秒；</li>
 *   <li>蓄满（4 秒，炽白压缩强化后 2 秒）：炽白火球，伤害 10，范围 4 米，冷却 8 秒。</li>
 * </ul>
 * 再次按技能键与右键等价；丢弃凝聚火球则蓄力散逸（由
 * {@link WarPathwayManager} 每刻同步物品与蓄力状态）。
 * 灵性与冷却在释放时结算（赤红 5 / 炽白 10 灵性），构造器填 0 手动写入。
 */
public class FireballAbility extends Ability {
    /** 蓄满阈值：4 秒；炽白压缩强化 -50%。 */
    private static final int FULL_CHARGE_TICKS = 80;
    private static final int RED_COST = 5;
    private static final int WHITE_COST = 10;
    private static final int RED_COOLDOWN = 3 * 20;
    private static final int WHITE_COOLDOWN = 8 * 20;

    public FireballAbility() {
        super(GuimiMod.id("fireball"), 0, 0, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        if (sp.getData(ModAttachments.FIREBALL_CHARGE_START) == 0L) {
            // 开始凝聚：火球握入主手，右键掷出
            sp.setData(ModAttachments.FIREBALL_CHARGE_START, level.getGameTime());
            WarPathwayManager.giveChargedOrb(sp, false);
            sp.displayClientMessage(Component.translatable("message.guimi_mod.fireball.charging"), true);
            return;
        }
        // 再次按技能键：与右键等价，直接释放
        release(sp);
    }

    /** 释放凝聚火球（右键凝聚火球物品 / 二次触发技能键时调用，服务端）。 */
    public static void release(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        long start = sp.getData(ModAttachments.FIREBALL_CHARGE_START);
        if (start == 0L) {
            return;
        }
        sp.setData(ModAttachments.FIREBALL_CHARGE_START, 0L);
        WarPathwayManager.removeChargedOrbs(sp, false);
        int fullTicks = SkillManager.isUnlocked(sp, GuimiMod.id("white_compression"))
                ? FULL_CHARGE_TICKS / 2 : FULL_CHARGE_TICKS;
        boolean white = now - start >= fullTicks;
        int cost = white ? WHITE_COST : RED_COST;
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < cost) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - cost);

        FlameOrbEntity orb = white
                ? new FlameOrbEntity(level, sp, 10.0F, 4.0F, true, false)
                : new FlameOrbEntity(level, sp, 5.0F, 2.0F, false, false);
        orb.setPos(sp.getX(), sp.getEyeY() - 0.2, sp.getZ());
        orb.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 1.5F, 1.0F);
        level.addFreshEntity(orb);
        level.playSound(null, sp.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, white ? 1.5F : 1.0F);
        sp.displayClientMessage(Component.translatable(
                white ? "message.guimi_mod.fireball.white" : "message.guimi_mod.fireball.red"), true);

        // 手动记录冷却（按火球档位区分）
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(GuimiMod.id("fireball"), now + (white ? WHITE_COOLDOWN : RED_COOLDOWN), now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
