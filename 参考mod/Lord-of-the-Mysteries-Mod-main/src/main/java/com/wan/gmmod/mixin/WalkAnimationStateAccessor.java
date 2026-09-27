package com.wan.gmmod.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 {@link WalkAnimationState} 的内部字段写入口。
 * <p>
 * 变形渲染时，需要把玩家的行走动画状态（位置 / 速度）精确拷贝到临时怪物实例上，
 * 以便怪物模型的四肢摆动与玩家保持同步。原类只提供 {@code setSpeed} 与只读的
 * {@code position()}，故通过 Accessor 直接写 {@code position}/{@code speedOld} 字段。
 */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {
    @Accessor("position")
    void gmmod$setPosition(float position);

    @Accessor("speed")
    void gmmod$setSpeed(float speed);

    @Accessor("speedOld")
    void gmmod$setSpeedOld(float speedOld);
}
