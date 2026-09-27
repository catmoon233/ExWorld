package com.wan.gmmod.common.network.packet;

import com.mojang.serialization.JsonOps;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import com.wan.gmmod.content.quest.Task;
import com.wan.gmmod.content.quest.TaskRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 任务定义同步包（服务端 → 客户端）。
 * <p>
 * 任务 JSON 仅存在于服务端数据包，客户端需要任务元数据（名称 / 描述 / 目标 / 奖励）
 * 来渲染任务书界面，故以 JSON 字符串形式整体下发。QuestData 进度本身由附件同步。
 * <p>
 * 注意：任务数量较多时序列化 JSON 可能超过 32KB（{@code ByteBufCodecs.STRING_UTF8}
 * 的默认上限），会导致同步失败、任务书空白，因此这里显式放宽到 1MB。
 */
public record QuestSyncPacket(String tasksJson) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuestSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(GuimiMod.id("quest_sync"));

    /** 长字符串编解码：默认 STRING_UTF8 上限 32KB，任务多时不够用，放宽到 1MB。 */
    public static final StreamCodec<FriendlyByteBuf, String> LONG_STRING =
            StreamCodec.of(
                    (buf, s) -> buf.writeUtf(s, 1024 * 1024),
                    buf -> buf.readUtf(1024 * 1024));

    public static final StreamCodec<FriendlyByteBuf, QuestSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    QuestSyncPacket.LONG_STRING, QuestSyncPacket::tasksJson,
                    QuestSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestSyncPacket msg, IPayloadContext context) {
        ClientPayloadDispatcher.handle(msg, context);
    }

    /** 服务端：把全部任务序列化为 JSON 数组字符串。 */
    public static String serialize() {
        var result = Task.CODEC.listOf().encodeStart(JsonOps.INSTANCE, TaskRegistry.all());
        return result.result().map(Object::toString).orElse("[]");
    }
}
