package net.exmo.exworld.mystery;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.exmo.exworld.command.TokenArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** DM controls and a small read-only player status command. */
public final class MysteryCommands {
    private MysteryCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("mystery");
        root.then(Commands.literal("status").executes(c -> {
            var data = MysterySave.get(c.getSource().getServer());
            c.getSource().sendSuccess(() -> Component.literal(data.phase + " · 第 " + data.cycle + " 轮 · 封印物 " + data.sealCount), false);
            return 1;
        }));
        root.then(Commands.literal("start").requires(s -> s.hasPermission(2)).executes(c ->
                MysteryGame.start(c.getSource().getServer()) ? 1 : 0));
        root.then(Commands.literal("lobby").requires(s -> s.hasPermission(2)).executes(c ->
                MysteryGame.prepareLobby(c.getSource().getServer()) ? 1 : 0));
        root.then(Commands.literal("rewind").requires(s -> s.hasPermission(2)).executes(c -> {
            MysteryGame.rewind(c.getSource().getServer()); return 1;
        }));
        root.then(Commands.literal("finish").requires(s -> s.hasPermission(2)).executes(c ->
                MysteryGame.resolveEnding(c.getSource().getServer()).isBlank() ? 0 : 1));
        root.then(Commands.literal("seal").requires(s -> s.hasPermission(2))
                .then(Commands.literal("set").then(Commands.argument("count", IntegerArgumentType.integer(0))
                        .executes(c -> {
                            MysteryGame.setSeals(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "count"));
                            return 1;
                        }))));
        root.then(Commands.literal("assign").requires(s -> s.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("era", TokenArgument.token())
                                .then(Commands.argument("character", TokenArgument.token())
                                        .then(Commands.argument("display", StringArgumentType.greedyString())
                                                .executes(c -> assign(EntityArgument.getPlayer(c, "player"),
                                                        StringArgumentType.getString(c, "era"),
                                                        StringArgumentType.getString(c, "character"),
                                                        StringArgumentType.getString(c, "display"))))))));
        root.then(Commands.literal("clue").requires(s -> s.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", TokenArgument.token())
                                .executes(c -> MysteryGame.discover(EntityArgument.getPlayer(c, "player"),
                                        StringArgumentType.getString(c, "id")) ? 1 : 0))));
        root.then(Commands.literal("blueprint").requires(s -> s.hasPermission(2))
                .then(Commands.argument("id", TokenArgument.token())
                        .executes(c -> {
                            net.exmo.exworld.mystery.blueprint.BlueprintEditorActions.open(c.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(c, "id"));
                            return 1;
                        })));
        event.getDispatcher().register(Commands.literal("exworld").then(root));
    }

    private static int assign(ServerPlayer player, String rawEra, String character, String display) {
        Era era;
        try { era = Era.valueOf(rawEra.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException error) { return 0; }
        return MysteryGame.assign(player, era, character, display) ? 1 : 0;
    }
}
