package com.wan.gmmod.common.command;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.QuestData;
import com.wan.gmmod.content.exp.ExperimentalPathways;
import com.wan.gmmod.content.quest.QuestManager;
import com.wan.gmmod.content.quest.Task;
import com.wan.gmmod.content.quest.TaskRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;

/**
 * 模组调试 / 管理命令。
 * <p>
 * {@code /guimi max [玩家...]}：将灵性与扮演进度直接拉满（均为 100），
 * 不带参数时作用于命令执行者本人。需要 OP 权限（等级 2）。
 * <p>
 * {@code /guimi demote [玩家...]}：消除途径、贬为凡人（清空序列 / 途径 / 扮演 / 灵性）。
 * <p>
 * {@code /guimi experimental <on|off|reset|status>}：临时开关实验性途径能力
 * （运行时覆盖，reset 恢复跟随配置文件）。
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public final class ModCommands {

    private ModCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("guimi")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("max")
                        .executes(ctx -> maxOut(ctx.getSource(),
                                List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> maxOut(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("experimental")
                        .then(Commands.literal("on").executes(ctx -> setExperimental(ctx.getSource(), Boolean.TRUE)))
                        .then(Commands.literal("off").executes(ctx -> setExperimental(ctx.getSource(), Boolean.FALSE)))
                        .then(Commands.literal("reset").executes(ctx -> setExperimental(ctx.getSource(), null)))
                        .then(Commands.literal("status").executes(ctx -> experimentalStatus(ctx.getSource()))))
                .then(Commands.literal("quest")
                        .then(Commands.literal("force")
                                .then(Commands.argument("task", ResourceLocationArgument.id())
                                        .executes(ctx -> forceAccept(ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "task")))))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("task", ResourceLocationArgument.id())
                                        .executes(ctx -> forceComplete(ctx.getSource(),
                                                ResourceLocationArgument.getId(ctx, "task")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> resetQuest(ctx.getSource(),
                                List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> resetQuest(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets"))))))
                .then(Commands.literal("city")
                        .then(Commands.literal("tingen")
                                .executes(ctx -> cityTingen(ctx.getSource())))
                        .then(Commands.literal("layout")
                                .executes(ctx -> cityLayout(ctx.getSource())))
                        .then(Commands.literal("markers")
                                .executes(ctx -> cityMarkers(ctx.getSource())))
                        .then(Commands.literal("unmark")
                                .executes(ctx -> cityUnmark(ctx.getSource())))
                        .then(Commands.literal("reset")
                                .executes(ctx -> cityReset(ctx.getSource())))
                        .then(Commands.literal("merge")
                                .executes(ctx -> cityMerge(ctx.getSource())))
                        .then(Commands.literal("reload")
                                .executes(ctx -> cityReload(ctx.getSource())))
                        .then(Commands.literal("sethere")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> citySetHere(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"), null))
                                        .then(Commands.argument("rotation", StringArgumentType.word())
                                                .executes(ctx -> citySetHere(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        StringArgumentType.getString(ctx, "rotation"))))))
                        .then(Commands.literal("setpos")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> citySetPos(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, "pos"), null))
                                                .then(Commands.argument("rotation", StringArgumentType.word())
                                                        .executes(ctx -> citySetPos(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "name"),
                                                                BlockPosArgument.getLoadedBlockPos(ctx, "pos"),
                                                                StringArgumentType.getString(ctx, "rotation"))))))))
                .then(Commands.literal("demote")
                        .executes(ctx -> demote(ctx.getSource(),
                                List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> demote(ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets"))))));
    }

    /** 跳过条件强制接取任务。 */
    private static int forceAccept(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        Task task = TaskRegistry.get(id);
        if (task == null) {
            source.sendFailure(Component.translatable("command.guimi_mod.quest.not_found", id));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        QuestManager.accept(player, task);
        return 1;
    }

    /** 跳过进度强制完成任务并发放奖励。 */
    private static int forceComplete(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        Task task = TaskRegistry.get(id);
        if (task == null) {
            source.sendFailure(Component.translatable("command.guimi_mod.quest.not_found", id));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        QuestData data = QuestManager.data(player);
        if (!data.hasActive(id.toString())) {
            QuestData withActive = data.withActive(id.toString());
            player.setData(ModAttachments.QUEST_DATA, withActive);
        }
        QuestManager.complete(player, task);
        return 1;
    }

    /** 清空玩家的任务进度 / 追踪 / 完成记录。 */
    private static int resetQuest(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            player.setData(ModAttachments.QUEST_DATA, QuestData.empty());
        }
        source.sendSuccess(() -> Component.translatable("command.guimi_mod.quest.reset", targets.size()), true);
        return targets.size();
    }

    /** 设置实验性途径能力的运行时开关（null = 恢复跟随配置文件）。 */
    private static int setExperimental(CommandSourceStack source, Boolean value) {
        ExperimentalPathways.setRuntimeOverride(value);
        return experimentalStatus(source);
    }

    /** 查询实验性途径能力当前状态。 */
    private static int experimentalStatus(CommandSourceStack source) {
        boolean enabled = ExperimentalPathways.isEnabled();
        source.sendSuccess(() -> Component.translatable(
                enabled ? "command.guimi_mod.experimental.on" : "command.guimi_mod.experimental.off"), true);
        return enabled ? 1 : 0;
    }

    /** 消除途径，贬为凡人：清空序列等级 / 途径 / 扮演进度与灵性。 */
    private static int demote(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            // 女巫「性别转换」：离开魔女途径（贬为凡人）时解除女性形态
            if (player.getData(ModAttachments.FEMALE_FORM)) {
                player.setData(ModAttachments.FEMALE_FORM, false);
                com.wan.gmmod.content.witch.FemaleGenderCompat.setFemale(player, false);
            }
            player.setData(ModAttachments.SEQUENCE_LEVEL, 0);
            player.setData(ModAttachments.PATHWAY, "");
            player.setData(ModAttachments.ACTING_SEQUENCE_ID, "");
            player.setData(ModAttachments.ACTING_PROGRESS, 0);
            SpiritualityManager.set(player, 0);
            player.displayClientMessage(
                    Component.translatable("command.guimi_mod.demote.applied"), false);
        }
        source.sendSuccess(() -> Component.translatable(
                "command.guimi_mod.demote.success", targets.size()), true);
        return targets.size();
    }

    /** 将目标玩家的灵性与扮演进度设为满值（灵性拉到当前动态上限）。 */
    private static int maxOut(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            int max = com.wan.gmmod.content.spirituality.SpiritualityManager.getMax(player);
            // 序列0上限无限时取一个实用的满值
            if (com.wan.gmmod.content.spirituality.SpiritualityManager.isInfinite(max)) {
                max = 10000;
            }
            SpiritualityManager.set(player, max);
            player.setData(ModAttachments.ACTING_PROGRESS, 100);
            player.displayClientMessage(
                    Component.translatable("command.guimi_mod.max.applied"), false);
        }
        source.sendSuccess(() -> Component.translatable(
                "command.guimi_mod.max.success", targets.size()), true);
        return targets.size();
    }

    private static int cityTingen(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        com.wan.gmmod.content.city.LoenCityGenerator.generateTingen(player.serverLevel(), player.blockPosition());
        source.sendSuccess(() -> Component.literal("已在当前位置生成廷根市"), true);
        return 1;
    }

    /** 打印每个结构规划的中心 / 锚点 / 尺寸，便于核对与调布局。 */
    private static int cityLayout(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        List<com.wan.gmmod.content.city.LoenCityGenerator.Planned> plan =
                com.wan.gmmod.content.city.LoenCityGenerator.plan(level);
        for (var p : plan) {
            source.sendSuccess(() -> Component.literal(
                    String.format("%s 中心=(%d,%d,%d) 尺寸=[%d,%d,%d] 锚点=(%d,%d,%d)",
                            p.name(), p.center().getX(), p.center().getY(), p.center().getZ(),
                            p.size()[0], p.size()[1], p.size()[2],
                            p.anchor().getX(), p.anchor().getY(), p.anchor().getZ())), false);
        }
        return plan.size();
    }

    /** 放置标记方块：金块=结构中心，青金石=锚点。返回实际数量。 */
    private static int cityMarkers(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int count = com.wan.gmmod.content.city.LoenCityGenerator.placeMarkers(level);
        source.sendSuccess(() -> Component.literal("已放置 " + count + " 个标记方块（金块=中心，青金石=锚点）"), false);
        return count;
    }

    /** 清除上次放置的标记方块。 */
    private static int cityUnmark(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        com.wan.gmmod.content.city.LoenCityGenerator.clearMarkers(level);
        source.sendSuccess(() -> Component.literal("已清除标记方块"), false);
        return 1;
    }

    /** 标记城市为未生成：下次服务器启动会按当前布局重新生成。 */
    private static int cityReset(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        com.wan.gmmod.content.city.LoenCityGenerator.resetGenerated(level);
        source.sendSuccess(() -> Component.literal("已标记：下次启动服务器时会重新生成城市（旧建筑不会自动清除）"), true);
        return 1;
    }

    /** 把当前布局的多个结构合并为单个 church_all.nbt，写入世界文件夹，不受结构方块 32 格上限影响。 */
    private static int cityMerge(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int blocks = com.wan.gmmod.content.city.LoenCityGenerator.mergeToSingle(level);
        if (blocks < 0) {
            source.sendFailure(Component.literal("合并失败，详见服务端日志"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "已合并为 church_all.nbt（" + blocks + " 个方块），存放在世界文件夹 guimi_structures/。"
                        + "执行 /guimi city reset 并重启服务器即可按合并结果生成"), true);
        return 1;
    }

    /** 免重启调位置：重载布局并立即重放标记。 */
    private static int cityReload(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        int count = com.wan.gmmod.content.city.LoenCityGenerator.reloadAndMark(level);
        source.sendSuccess(() -> Component.literal("已重载布局并更新标记（" + count + " 个标记方块）"), false);
        return 1;
    }

    /** 站在目标位置调整结构：中心 x/z 取脚下，y 保持当前值。 */
    private static int citySetHere(CommandSourceStack source, String name, String rotation) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        Player player = source.getPlayerOrException();
        int r = com.wan.gmmod.content.city.LoenCityGenerator.setHere(level, name, player.blockPosition(), rotation);
        if (r < 0) {
            source.sendFailure(Component.literal("结构 " + name + " 不存在，可用 /guimi city layout 查看"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "已把 " + name + " 的中心 x/z 设到你脚下（y 保持原值）" + (rotation != null ? "，rotation=" + rotation : "")
                        + "，标记已更新"), true);
        return 1;
    }

    /** 用世界坐标指定结构的中心位置。 */
    private static int citySetPos(CommandSourceStack source, String name, BlockPos pos, String rotation) {
        ServerLevel level = source.getLevel();
        int r = com.wan.gmmod.content.city.LoenCityGenerator.setPos(level, name, pos, rotation);
        if (r < 0) {
            source.sendFailure(Component.literal("结构 " + name + " 不存在，可用 /guimi city layout 查看"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "已把 " + name + " 的中心设到 " + pos.toShortString() + (rotation != null ? "，rotation=" + rotation : "")
                        + "，标记已更新"), true);
        return 1;
    }
}
