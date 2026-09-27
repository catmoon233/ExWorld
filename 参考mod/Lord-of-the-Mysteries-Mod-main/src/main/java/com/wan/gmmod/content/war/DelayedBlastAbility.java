package com.wan.gmmod.content.war;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.entities.FlameTrapEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 「延时爆炸」——纵火家（战争之红途径 · 序列 7）主动。
 * <p>
 * 放置一个火焰陷阱，5 秒后爆炸（伤害 8，范围 3 米）；陷阱存活期间
 * 再次触发本技能可提前引爆。冷却 15 秒，在陷阱爆炸时由
 * {@link FlameTrapEntity#detonate()} 写入（构造器填 0）。
 */
public class DelayedBlastAbility extends Ability {
    private static final int COST = 8;

    public DelayedBlastAbility() {
        super(GuimiMod.id("delayed_blast"), 0, 0, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        // 已有存活陷阱：提前引爆
        List<FlameTrapEntity> traps = level.getEntitiesOfClass(FlameTrapEntity.class,
                sp.getBoundingBox().inflate(30.0), t -> t.getOwner() == sp);
        if (!traps.isEmpty()) {
            traps.get(0).detonate();
            return;
        }
        int spirituality = SpiritualityManager.get(sp);
        if (spirituality < COST) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.skill.no_spirituality"), true);
            return;
        }
        SpiritualityManager.set(sp, spirituality - COST);
        FlameTrapEntity trap = new FlameTrapEntity(level, sp);
        trap.setPos(sp.getX(), sp.getEyeY() - 0.2, sp.getZ());
        trap.shootFromRotation(sp, sp.getXRot(), sp.getYRot(), 0.0F, 0.8F, 1.0F);
        level.addFreshEntity(trap);
        level.playSound(null, sp.blockPosition(),
                SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.6F, 1.4F);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.delayed_blast.placed"), true);
    }
}
