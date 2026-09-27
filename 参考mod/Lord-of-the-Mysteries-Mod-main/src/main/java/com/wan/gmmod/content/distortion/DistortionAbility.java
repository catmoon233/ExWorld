package com.wan.gmmod.content.distortion;

import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.GuimiMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 扭曲模式主动能力（腐化男爵完整版 / 门、命运之轮弱化版）。
 * <p>
 * 触发后进入「扭曲模式」窗口（15 秒），玩家通过客户端 UI（准星高亮目标 / 右键拖拽区域）
 * 选择扭曲目标，随后发送 {@code DistortionCastPacket} 完成施放。
 * <p>
 * 三种注册能力：
 * <ul>
 *   <li>{@code be_distortion}（黑皇帝·序列6 腐化男爵，完整版）：全部 6 类扭曲；</li>
 *   <li>{@code door_distortion}（门·序列6 记录官，弱化版）：移动反向 / 封闭屏障 / 隔绝房间；</li>
 *   <li>{@code whl_distortion}（命运之轮·序列6 灾祸教士，弱化版）：攻击转移 / 占卜劫持。</li>
 * </ul>
 */
public class DistortionAbility extends Ability {

    public DistortionAbility(String path, int cost, int cdSecs) {
        super(GuimiMod.id(path), cost, cdSecs * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            // 重复触发视为退出模式
            if (DistortionManager.isInMode(sp)) {
                DistortionManager.exitMode(sp);
                return;
            }
            DistortionManager.enterMode(sp);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.guimi_mod.distortion.mode_on"), true);
        }
    }

    /** 注册三个版本的扭曲能力（黑皇帝·序列6 完整版；门 / 命运之轮·序列6 弱化版）。 */
    public static void init() {
        AbilityRegistry.register(GuimiMod.id("black_emperor_6"),
                new DistortionAbility("be_distortion", 30, 20));
        AbilityRegistry.register(GuimiMod.id("door_6"),
                new DistortionAbility("door_distortion", 20, 20));
        AbilityRegistry.register(GuimiMod.id("wheel_6"),
                new DistortionAbility("whl_distortion", 20, 20));
    }
}