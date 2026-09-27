package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.marionette.MarionetteManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 秘偶大师「共享视野」——序列 5 主动能力。
 * <p>
 * 秘偶化完成后按技能键切换视野：摄像机绑定到秘偶实体，玩家本体挂机
 * （无法移动，保留最小交互），WASD 操控秘偶移动、空格跳跃、潜行键退出，
 * 左键近战攻击、右键触发秘偶保留的原有非凡能力。
 * 服务端核心逻辑见 {@link MarionetteManager#toggleControl}。
 */
public class SharedVisionAbility extends Ability {

    public SharedVisionAbility() {
        // 消耗 5 灵性，冷却 20 刻（1 秒），主动能力
        super(GuimiMod.id("shared_vision"), 5, 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            MarionetteManager.toggleControl(serverPlayer);
        }
    }
}
