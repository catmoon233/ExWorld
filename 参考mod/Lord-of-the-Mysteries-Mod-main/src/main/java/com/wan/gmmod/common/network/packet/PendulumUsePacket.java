package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 黄水晶灵摆使用网络包（服务端 → 客户端）。
 * <p>
 * 玩家右键灵摆时，服务端先执行占卜，再向所有追踪该玩家的客户端（含自己）
 * 广播本包，携带使用者的实体 ID 与占卜结果。客户端收到后同时驱动：
 * <ul>
 *     <li>手臂旋转：{@link PendulumClientState} 记录该玩家正在使用灵摆，
 *     由 {@code PlayerModelMixin} 在渲染时把右臂旋转到胸前；</li>
 *     <li>物品动画：{@link PendulumItem} 根据摆动方向播放顺时针 / 逆时针 / 静止动画。</li>
 * </ul>
 */
public record PendulumUsePacket(int entityId, int spinId) implements CustomPacketPayload {
    /** 动画 / 手臂抬起持续时间（tick），客户端计时使用 */
    public static final int DURATION = 40;

    public static final Type<PendulumUsePacket> TYPE = new Type<>(GuimiMod.id("pendulum_use"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PendulumUsePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PendulumUsePacket::entityId,
                    ByteBufCodecs.VAR_INT, PendulumUsePacket::spinId,
                    PendulumUsePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理：仅在客户端注册（playToClient），因此这里的逻辑只会在客户端执行。
     */
    public static void handle(PendulumUsePacket msg, IPayloadContext ctx) {
        ClientPayloadDispatcher.handle(msg, ctx);
    }
}
