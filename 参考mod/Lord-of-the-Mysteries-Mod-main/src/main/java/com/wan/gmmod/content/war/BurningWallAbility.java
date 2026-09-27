package com.wan.gmmod.content.war;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityTargeting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 「燃烧之墙」——纵火家（战争之红途径 · 序列 7）主动。
 * <p>
 * 在视线落点（20 米内）升起一圈半径 3 米的火墙，持续 8 秒，
 * 穿越者受到 5 点火焰伤害 + 燃烧（逻辑在 {@link FireWallManager}）。
 * 冷却 25 秒（选中落点才结算，构造器填 0 手动写入）。
 */
public class BurningWallAbility extends Ability {
    private static final double RANGE = 20.0;
    private static final int DURATION = 8 * 20;
    private static final int COST = 10;
    private static final int COOLDOWN = 25 * 20;

    public BurningWallAbility() {
        super(GuimiMod.id("burning_wall"), 0, 0, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        BlockHitResult hit = AbilityTargeting.pickBlock(sp, RANGE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.burning_wall.no_target"), true);
            return;
        }
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - COST);

        Vec3 center = hit.getLocation();
        FireWallManager.addWall(level, center, sp.getUUID(), DURATION);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.burning_wall.done"), true);

        long now = level.getGameTime();
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(getId(), now + COOLDOWN, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
