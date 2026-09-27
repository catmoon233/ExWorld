package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.quest.QuestManager;
import com.wan.gmmod.content.quest.Task;
import com.wan.gmmod.content.quest.TaskRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 任务书操作包（客户端 → 服务端）。
 * <p>
 * 在任务书界面点击「接取 / 放弃 / 追踪」时发送。服务端根据 {@code action} 执行对应逻辑，
 * 并校验任务是否可执行（不可放弃主线等），防作弊。
 *
 * @param action 操作类型：accept / abandon / track
 * @param taskId 任务 ID 字符串
 */
public record QuestActionPacket(String action, String taskId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuestActionPacket> TYPE =
            new CustomPacketPayload.Type<>(GuimiMod.id("quest_action"));

    public static final StreamCodec<FriendlyByteBuf, QuestActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, QuestActionPacket::action,
                    ByteBufCodecs.STRING_UTF8, QuestActionPacket::taskId,
                    QuestActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestActionPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) {
                return;
            }
            ResourceLocation id = ResourceLocation.tryParse(msg.taskId());
            if (id == null) {
                return;
            }
            Task task = TaskRegistry.get(id);
            if (task == null) {
                return;
            }
            switch (msg.action()) {
                case "accept" -> QuestManager.accept(sp, task);
                case "abandon" -> QuestManager.abandon(sp, task);
                case "track" -> QuestManager.toggleTrack(sp, task);
                default -> { }
            }
        });
    }
}