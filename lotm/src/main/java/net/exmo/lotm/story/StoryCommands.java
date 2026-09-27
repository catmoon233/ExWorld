package net.exmo.lotm.story;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class StoryCommands {
    private StoryCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("exworld").then(Commands.literal("story")
                .then(Commands.literal("anchor").requires(source -> source.hasPermission(2))
                        .executes(context -> anchor(context.getSource())))
                .then(Commands.literal("pair").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> pair(context.getSource(), EntityArgument.getPlayer(context, "target")))))));
    }

    private static int anchor(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StoryData.get(player).anchor(player);
        source.sendSuccess(() -> Component.translatable("item.lotm.sealed_key.anchored"), true);
        return 1;
    }

    private static int pair(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer self = source.getPlayerOrException();
        if (self == target) {
            source.sendFailure(Component.translatable("command.lotm.story.pair_self"));
            return 0;
        }
        StoryActions.pair(self, target);
        source.sendSuccess(() -> Component.translatable("command.lotm.story.paired", target.getName()), true);
        return 1;
    }
}
