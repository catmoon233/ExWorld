package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 「轻盈步伐」——刺客（魔女途径 · 序列 9）被动。
 * <ul>
 *   <li>常驻 跳跃提升 I + 缓降；</li>
 *   <li>免疫摔落伤害（见 {@code WitchAbilityEventSubscriber#onFall}）；</li>
 *   <li>疾跑时移动速度 +10%（瞬态属性修饰符，停止疾跑即移除）。</li>
 * </ul>
 */
public class LightFootstepsAbility extends Ability {
    /** 疾跑加速属性修饰符 ID */
    private static final ResourceLocation SPRINT_MODIFIER_ID = GuimiMod.id("light_footsteps_sprint");

    public LightFootstepsAbility() {
        super(GuimiMod.id("light_footsteps"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        // 常驻跳跃提升 I + 缓降（缓降天然免疫大部分摔落，事件里再兜底完全免疫）
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 220, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, true, false));

        // 疾跑速度 +10%：疾跑时挂上瞬态修饰符，停止时移除
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean has = speed.getModifier(SPRINT_MODIFIER_ID) != null;
        if (player.isSprinting() && !has) {
            speed.addTransientModifier(new AttributeModifier(SPRINT_MODIFIER_ID, 0.10,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!player.isSprinting() && has) {
            speed.removeModifier(SPRINT_MODIFIER_ID);
        }
    }

    @Override
    public void onDeactivate(Player player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPRINT_MODIFIER_ID);
        }
    }
}
