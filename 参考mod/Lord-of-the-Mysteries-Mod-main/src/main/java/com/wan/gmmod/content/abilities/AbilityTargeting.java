package com.wan.gmmod.content.abilities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * 能力目标拾取工具：沿玩家视线方向拾取实体 / 方块。
 * <p>
 * 供火焰跳跃、操纵火焰、灵体之线操控等指向性能力复用。
 */
public final class AbilityTargeting {
    private AbilityTargeting() {}

    /** 沿视线拾取 range 内最近的活体实体（包含隐身目标，反隐），未命中返回 null。 */
    public static LivingEntity pickLivingEntity(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        Predicate<Entity> filter = e -> e instanceof LivingEntity && e != player && e.isAlive();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player, start, end, box, filter);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** 沿视线拾取 range 内的方块命中结果（可能为 MISS，调用方自行判断类型）。 */
    public static BlockHitResult pickBlock(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(range));
        return player.level().clip(new ClipContext(start, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }
}
