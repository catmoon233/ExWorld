package com.wan.gmmod.common.network;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.packet.HighlightBlocksPacket;
import com.wan.gmmod.common.network.packet.MeditationSyncPacket;
import com.wan.gmmod.common.network.packet.MeditationTogglePacket;
import com.wan.gmmod.common.network.packet.MirrorDivinationPacket;
import com.wan.gmmod.common.network.packet.PendulumUsePacket;
import com.wan.gmmod.common.network.packet.SelectDisguisePacket;
import com.wan.gmmod.common.network.packet.SilenceGunFirePacket;
import com.wan.gmmod.common.network.packet.SpiritWallTogglePacket;
import com.wan.gmmod.common.network.packet.StealChoicePacket;
import com.wan.gmmod.common.network.packet.StealMenuPacket;
import com.wan.gmmod.common.network.packet.ToggleSpiritVisionPacket;
import com.wan.gmmod.common.network.packet.ConfigureSkillPacket;
import com.wan.gmmod.common.network.packet.TriggerSkillPacket;
import com.wan.gmmod.common.network.packet.UseAbilityPacket;
import com.wan.gmmod.common.network.packet.ToggleCardModePacket;
import com.wan.gmmod.common.network.packet.MarionetteViewPacket;
import com.wan.gmmod.common.network.packet.MarionetteControlInputPacket;
import com.wan.gmmod.common.network.packet.MarionetteActionPacket;
import com.wan.gmmod.common.network.packet.SpiritThreadSyncPacket;
import com.wan.gmmod.common.network.packet.QuestActionPacket;
import com.wan.gmmod.common.network.packet.QuestSyncPacket;
import com.wan.gmmod.common.network.packet.CocoonSyncPacket;
import com.wan.gmmod.common.network.packet.DistortionCastPacket;
import com.wan.gmmod.common.network.packet.DistortionModeSyncPacket;
import com.wan.gmmod.common.network.packet.DistortionZoneSyncPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = GuimiMod.MODID)
public class ModNetwork {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                UseAbilityPacket.TYPE,
                UseAbilityPacket.STREAM_CODEC,
                UseAbilityPacket::handle
        );
        // 灵摆使用包：服务端 → 客户端，通知客户端播放动画并旋转手臂
        registrar.playToClient(
                PendulumUsePacket.TYPE,
                PendulumUsePacket.STREAM_CODEC,
                PendulumUsePacket::handle
        );
        // 灵视切换包：客户端 → 服务端，翻转灵视开关
        registrar.playToServer(
                ToggleSpiritVisionPacket.TYPE,
                ToggleSpiritVisionPacket.STREAM_CODEC,
                ToggleSpiritVisionPacket::handle
        );
        // 方块高亮包：服务端 → 客户端，登记需描边的矿石 / 宝箱
        registrar.playToClient(
                HighlightBlocksPacket.TYPE,
                HighlightBlocksPacket.STREAM_CODEC,
                HighlightBlocksPacket::handle
        );
        // 技能栏触发包：客户端 → 服务端，按快捷键触发指定槽位能力
        registrar.playToServer(
                TriggerSkillPacket.TYPE,
                TriggerSkillPacket.STREAM_CODEC,
                TriggerSkillPacket::handle
        );
        // 技能栏配置包：客户端 → 服务端，将能力指派 / 清空槽位
        registrar.playToServer(
                ConfigureSkillPacket.TYPE,
                ConfigureSkillPacket.STREAM_CODEC,
                ConfigureSkillPacket::handle
        );
        // 灵性之墙切换包：客户端 → 服务端，X 键切换灵性之墙
        registrar.playToServer(
                SpiritWallTogglePacket.TYPE,
                SpiritWallTogglePacket.STREAM_CODEC,
                SpiritWallTogglePacket::handle
        );
        // 冥想切换包：客户端 → 服务端，P 键开始/结束冥想
        registrar.playToServer(
                MeditationTogglePacket.TYPE,
                MeditationTogglePacket.STREAM_CODEC,
                MeditationTogglePacket::handle
        );
        // 冥想状态同步包：服务端 → 客户端，锁定/解除冥想姿态
        registrar.playToClient(
                MeditationSyncPacket.TYPE,
                MeditationSyncPacket.STREAM_CODEC,
                MeditationSyncPacket::handle
        );
        // 寂灭开火包：客户端 → 服务端，左键空挥开枪
        registrar.playToServer(
                SilenceGunFirePacket.TYPE,
                SilenceGunFirePacket.STREAM_CODEC,
                SilenceGunFirePacket::handle
        );
        // 变形选择包：客户端 → 服务端，在变形界面选择 / 取消怪物外观
        registrar.playToServer(
                SelectDisguisePacket.TYPE,
                SelectDisguisePacket.STREAM_CODEC,
                SelectDisguisePacket::handle
        );
        // 纸牌模式切换包：客户端 → 服务端，B 键切换精准 / 散射
        registrar.playToServer(
                ToggleCardModePacket.TYPE,
                ToggleCardModePacket.STREAM_CODEC,
                ToggleCardModePacket::handle
        );
        // 秘偶共享视野包：服务端 → 客户端，开启 / 关闭摄像机绑定
        registrar.playToClient(
                MarionetteViewPacket.TYPE,
                MarionetteViewPacket.STREAM_CODEC,
                MarionetteViewPacket::handle
        );
        // 秘偶操控输入包：客户端 → 服务端，WASD / 跳跃 / 视角
        registrar.playToServer(
                MarionetteControlInputPacket.TYPE,
                MarionetteControlInputPacket.STREAM_CODEC,
                MarionetteControlInputPacket::handle
        );
        // 秘偶操控动作包：客户端 → 服务端，退出 / 近战攻击 / 触发能力
        registrar.playToServer(
                MarionetteActionPacket.TYPE,
                MarionetteActionPacket.STREAM_CODEC,
                MarionetteActionPacket::handle
        );
        // 灵体之线同步包：服务端 → 客户端，下发秘偶 / 目标 id 与挣扎状态供连线渲染
        registrar.playToClient(
                SpiritThreadSyncPacket.TYPE,
                SpiritThreadSyncPacket.STREAM_CODEC,
                SpiritThreadSyncPacket::handle
        );
        // 魔镜占卜包：客户端 → 服务端，选择占卜 / 反占卜 / 通灵模式
        registrar.playToServer(
                MirrorDivinationPacket.TYPE,
                MirrorDivinationPacket.STREAM_CODEC,
                MirrorDivinationPacket::handle
        );
        // 任务书操作包：客户端 → 服务端，接取 / 放弃 / 追踪任务
        registrar.playToServer(
                QuestActionPacket.TYPE,
                QuestActionPacket.STREAM_CODEC,
                QuestActionPacket::handle
        );
        // 任务定义同步包：服务端 → 客户端，下发任务元数据供任务书渲染
        registrar.playToClient(
                QuestSyncPacket.TYPE,
                QuestSyncPacket.STREAM_CODEC,
                QuestSyncPacket::handle
        );
        // 蛛丝蚕茧同步包：服务端 → 客户端，驱动外壳 / 第一人称滤网渐变
        registrar.playToClient(
                CocoonSyncPacket.TYPE,
                CocoonSyncPacket.STREAM_CODEC,
                CocoonSyncPacket::handle
        );
        // 扭曲施放包：客户端 → 服务端，扭曲模式下确认目标 / 区域
        registrar.playToServer(
                DistortionCastPacket.TYPE,
                DistortionCastPacket.STREAM_CODEC,
                DistortionCastPacket::handle
        );
        // 扭曲模式状态包：服务端 → 客户端，进入 / 退出扭曲 UI
        registrar.playToClient(
                DistortionModeSyncPacket.TYPE,
                DistortionModeSyncPacket.STREAM_CODEC,
                DistortionModeSyncPacket::handle
        );
        // 扭曲区域同步包：服务端 → 客户端，下发封闭屏障 / 隔绝房间边界供描边
        registrar.playToClient(
                DistortionZoneSyncPacket.TYPE,
                DistortionZoneSyncPacket.STREAM_CODEC,
                DistortionZoneSyncPacket::handle
        );
        // 盗火人隔空盗窃菜单包：服务端 → 客户端，打开偷取选择界面
        registrar.playToClient(
                StealMenuPacket.TYPE,
                StealMenuPacket.STREAM_CODEC,
                StealMenuPacket::handle
        );
        // 盗火人隔空盗窃选择包：客户端 → 服务端，回传所选物品下标
        registrar.playToServer(
                StealChoicePacket.TYPE,
                StealChoicePacket.STREAM_CODEC,
                StealChoicePacket::handle
        );
    }
}