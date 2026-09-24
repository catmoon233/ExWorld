package net.exmo.lotm.sequence;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SequenceCommands {
    private SequenceCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("exworld").then(Commands.literal("sequence")
                .then(Commands.literal("set").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("pathway", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                SequenceRegistry.pathwayIds(), builder))
                                        .then(Commands.argument("rank", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        SequenceRegistry.rankSuggestions(
                                                                ResourceLocationArgument.getId(context, "pathway")), builder))
                                                .executes(context -> set(
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        ResourceLocationArgument.getId(context, "pathway"),
                                                        StringArgumentType.getString(context, "rank"),
                                                        context.getSource()))))))
                .then(Commands.literal("clear").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(context -> {
                            int count = 0;
                            for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) {
                                SequenceService.clear(player);
                                count++;
                                context.getSource().sendSuccess(() -> Component.translatable(
                                        "command.exworld.sequence.clear", player.getDisplayName()), true);
                            }
                            return count;
                        })))
                .then(Commands.literal("get")
                        .executes(context -> get(context.getSource().getPlayerOrException(), context.getSource()))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(context -> {
                            int count = 0;
                            for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) {
                                report(context.getSource(), player);
                                count++;
                            }
                            return count;
                        })))
                .then(Commands.literal("pathways").executes(context -> {
                    var ids = SequenceRegistry.pathwaySuggestions();
                    if (ids.isEmpty()) {
                        context.getSource().sendFailure(Component.translatable("command.exworld.sequence.no_pathways"));
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> Component.translatable(
                            "command.exworld.sequence.pathways", String.join(", ", ids)), false);
                    return ids.size();
                }))
                .then(Commands.literal("open").executes(context -> {
                    SequenceNetwork.sync(context.getSource().getPlayerOrException(), true);
                    return 1;
                }))));
    }

    private static int set(Iterable<ServerPlayer> players, ResourceLocation pathwayId, String rankRaw,
                           net.minecraft.commands.CommandSourceStack source) {
        PathwayDefinition pathway = SequenceRegistry.findPathway(pathwayId).orElse(null);
        if (pathway == null) {
            var known = SequenceRegistry.pathwaySuggestions();
            source.sendFailure(Component.translatable("command.exworld.sequence.unknown_pathway", pathwayId.toString()));
            source.sendFailure(Component.translatable(
                    known.isEmpty() ? "command.exworld.sequence.no_pathways" : "command.exworld.sequence.pathways",
                    String.join(", ", known)));
            return 0;
        }
        SequenceRank rank = SequenceRank.parse(rankRaw).orElse(null);
        if (rank == null) {
            source.sendFailure(Component.translatable("command.exworld.sequence.unknown_rank", rankRaw));
            return 0;
        }
        if (pathway.byRank(rank) == null) {
            source.sendFailure(Component.translatable("command.exworld.sequence.rank_missing", pathway.id().toString(), rank.token()));
            return 0;
        }
        int count = 0;
        for (ServerPlayer player : players) {
            SequenceService.set(player, pathway, rank);
            SequenceDefinition current = pathway.byRank(rank);
            source.sendSuccess(() -> Component.translatable(
                    "command.exworld.sequence.set",
                    player.getDisplayName(),
                    Component.translatable(current.nameKey()),
                    Component.translatable(current.rank().translationKey())), true);
            count++;
        }
        return count;
    }

    private static int get(ServerPlayer player, net.minecraft.commands.CommandSourceStack source) {
        report(source, player);
        return 1;
    }

    private static void report(net.minecraft.commands.CommandSourceStack source, ServerPlayer player) {
        SequenceDefinition current = SequenceService.current(player).orElse(null);
        if (current == null) {
            source.sendSuccess(() -> Component.translatable(
                    "command.exworld.sequence.get",
                    player.getDisplayName(),
                    Component.translatable("command.exworld.sequence.none"),
                    Component.translatable("command.exworld.sequence.none")), false);
            return;
        }
        source.sendSuccess(() -> Component.translatable(
                "command.exworld.sequence.get",
                player.getDisplayName(),
                Component.translatable(current.nameKey()),
                Component.translatable(current.rank().translationKey())), false);
    }
}
