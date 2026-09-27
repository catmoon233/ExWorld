package com.wan.gmmod.content.entities;

import com.wan.gmmod.common.registry.ModSounds;
import com.wan.gmmod.content.ritual.MermaidSongManager;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * 美人鱼实体（GeckoLib 动画水生生物）。
 * <p>
 * 海洋生物群系中低概率自然生成（见 {@code data/guimi_mod/neoforge/biome_modifier/add_mermaid_spawn.json}）。
 * 两种动画：
 * <ul>
 *     <li>行动动画 {@code animation.mermaid.move}（循环，游动 / 待机）；</li>
 *     <li>技能动画 {@code animation.mermaid.sing}（唱歌时循环播放）。</li>
 * </ul>
 * 技能「美人鱼的歌声」：周期性开始唱歌（播放技能动画与歌声音效），
 * 并持续对 16 格内的玩家施加隐藏的真歌声 Buff（{@code guimi_mod:mermaid_song}）、
 * 累计暴露时间——停留满 30 秒的玩家获得永久的「亲耳听过真歌声」标记。
 * <p>
 * 模型 / 纹理 / 动画 / 音频由用户自行提供（见 {@code MermaidModel} 与 sounds.json）。
 */
public class MermaidEntity extends WaterAnimal implements GeoEntity {
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("animation.mermaid.move");
    private static final RawAnimation SING = RawAnimation.begin().thenLoop("animation.mermaid.sing");

    /** 是否正在唱歌（同步到客户端驱动技能动画） */
    private static final EntityDataAccessor<Boolean> SINGING =
            SynchedEntityData.defineId(MermaidEntity.class, EntityDataSerializers.BOOLEAN);

    /** 歌声 Buff 覆盖半径（格） */
    public static final double SONG_RANGE = 16.0;
    /** 每次唱歌持续时间（刻） */
    private static final int SING_DURATION = 100;
    /** 两次唱歌的间隔（刻） */
    private static final int SING_INTERVAL = 300;
    /** Buff 刷新间隔（刻） */
    private static final int BUFF_REFRESH_INTERVAL = 40;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** 距下次开始唱歌的倒计时 */
    private int singCooldown = 100;
    /** 本次唱歌剩余时间 */
    private int singTicks;

    public MermaidEntity(EntityType<? extends WaterAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WaterAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SINGING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0, 10));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public boolean isSinging() {
        return this.entityData.get(SINGING);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        // 死亡 / 濒死期间不再推进唱歌状态机：
        // 否则死亡动画期间 singCooldown 已 <= 0，会立即重新开唱、抵消停音包
        if (this.isDeadOrDying()) {
            return;
        }

        // 唱歌节奏：周期性开始唱歌（技能动画 + 歌声音效）
        if (this.singTicks > 0) {
            if (--this.singTicks == 0) {
                this.entityData.set(SINGING, false);
                this.singCooldown = SING_INTERVAL;
            }
        } else if (--this.singCooldown <= 0) {
            this.entityData.set(SINGING, true);
            this.singTicks = SING_DURATION;
            this.level().playSound(null, this.blockPosition(), ModSounds.MERMAID_SONG.get(),
                    SoundSource.NEUTRAL, 1.2F, 0.95F + this.random.nextFloat() * 0.1F);
        }

        // 歌声效果：持续覆盖 16 格内的玩家（无论是否正处于唱歌动画中）
        List<Player> players = this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(SONG_RANGE));
        for (Player player : players) {
            MermaidSongManager.tickExposure(player);
            if (this.tickCount % BUFF_REFRESH_INTERVAL == 0) {
                MermaidSongManager.applyRealSong(player);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.stopSong();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // 远离玩家 despawn / discard 不会走 die()，同样需要停歌
        if (!this.level().isClientSide && reason.shouldDestroy()) {
            this.stopSong();
        }
        super.remove(reason);
    }

    /** 停止唱歌并向附近玩家下发停音包：歌声为流式长音频，需主动中断客户端播放 */
    private void stopSong() {
        this.entityData.set(SINGING, false);
        this.singTicks = 0;
        this.singCooldown = SING_INTERVAL;
        if (this.level() instanceof ServerLevel level) {
            ClientboundStopSoundPacket stop = new ClientboundStopSoundPacket(
                    ModSounds.MERMAID_SONG.get().getLocation(), SoundSource.NEUTRAL);
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(this) <= 64.0 * 64.0) {
                    player.connection.send(stop);
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            // 唱歌时播放技能动画，其余时间循环行动动画
            if (this.isSinging()) {
                return state.setAndContinue(SING);
            }
            return state.setAndContinue(MOVE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
