package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * 火焰陷阱——纵火家（战争之红途径 · 序列 7）「延时爆炸」放置的陷阱。
 * <p>
 * 落地后静置，5 秒（100 刻）引信结束时爆炸：伤害 8，范围 3 米。
 * 再次触发「延时爆炸」技能可提前引爆（见 DelayedBlastAbility）。
 */
public class FlameTrapEntity extends ThrowableItemProjectile {
    /** 引信：5 秒。 */
    private static final int FUSE_TICKS = 100;
    private int fuse;
    private boolean landed;

    public FlameTrapEntity(EntityType<? extends FlameTrapEntity> type, Level level) {
        super(type, level);
    }

    public FlameTrapEntity(Level level, LivingEntity shooter) {
        super(ModEntities.FLAME_TRAP.get(), shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.MAGMA_CREAM;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SMALL_FLAME,
                        this.getX(), this.getY() + 0.2, this.getZ(), 0, 0.02, 0);
            }
            return;
        }
        if (this.landed && ++this.fuse >= FUSE_TICKS) {
            this.detonate();
        }
    }

    /** 立即引爆（引信结束或纵火家主动引爆），并为主人写入「延时爆炸」15 秒冷却。 */
    public void detonate() {
        if (!this.level().isClientSide && this.isAlive()) {
            FlameOrbEntity.explode(this.level(), this.position(),
                    this.getOwner() instanceof LivingEntity living ? living : null,
                    8.0F, 3.0F, false);
            this.level().broadcastEntityEvent(this, (byte) 3);
            // 冷却自爆炸时起算，使「提前引爆」不被冷却拦截
            if (this.getOwner() instanceof net.minecraft.server.level.ServerPlayer sp) {
                long now = this.level().getGameTime();
                com.wan.gmmod.common.capability.data.CooldownData cd =
                        sp.getData(com.wan.gmmod.common.capability.ModAttachments.SKILL_COOLDOWNS)
                                .with(com.wan.gmmod.GuimiMod.id("delayed_blast"), now + 15 * 20, now);
                sp.setData(com.wan.gmmod.common.capability.ModAttachments.SKILL_COOLDOWNS, cd);
            }
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        // 落地后静置计时，不销毁；命中生物不触发（陷阱仅由引信/主动引爆）
        if (!this.level().isClientSide && result.getType() == HitResult.Type.BLOCK) {
            this.landed = true;
            this.setDeltaMovement(0, 0, 0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 24; i++) {
                this.level().addParticle(ParticleTypes.FLAME,
                        this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.7,
                        this.random.nextDouble() * 0.4,
                        (this.random.nextDouble() - 0.5) * 0.7);
            }
            this.level().addParticle(ParticleTypes.EXPLOSION,
                    this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        } else {
            super.handleEntityEvent(id);
        }
    }
}
