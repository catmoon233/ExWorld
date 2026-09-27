package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 纸人：由纸人物品右键放置的静态替身实体。
 * <p>
 * 无 AI、不移动，作为魔术师 / 无面人「纸人替身」能力的交换锚点：
 * 技能触发时玩家与 10 米内最近的纸人交换位置，纸人随之被消耗。
 * 被打碎时掉落纸人物品本体。
 */
public class PaperFigurineEntity extends PathfinderMob {

    public PaperFigurineEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        // 纸人没有任何 AI 行为
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.PAPER_FIGURINE.get()));
    }
}
