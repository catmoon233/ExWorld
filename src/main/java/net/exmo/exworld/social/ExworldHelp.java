package net.exmo.exworld.social;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.exmo.exworld.command.TokenArgument;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /exworld help} and the nearby-chat screen command. */
public final class ExworldHelp {
    private ExworldHelp() {}

    public static void register(RegisterCommandsEvent event) {
        ExworldHelpCatalog.ensureBuiltins();
        event.getDispatcher().register(Commands.literal("exworld")
                .then(Commands.literal("help")
                        .executes(context -> show(context.getSource(), ""))
                        .then(Commands.argument("topic", TokenArgument.token())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ExworldHelpCatalog.ids(), builder))
                                .executes(context -> show(context.getSource(), StringArgumentType.getString(context, "topic")))))
                .then(Commands.literal("chat").executes(context -> NearbyChat.open(context.getSource()))));
    }

    /** Other mods register extra topics from their own command registrar. See docs/exworld-help.md. */
    public static void topic(String id, String title, ExworldHelpCatalog.Line... lines) {
        ExworldHelpCatalog.topic(id, title, lines);
    }

    public static ExworldHelpCatalog.Line line(String usage, String detail) {
        return ExworldHelpCatalog.line(usage, detail);
    }

    private static int show(CommandSourceStack source, String topicId) {
        ExworldHelpCatalog.ensureBuiltins();
        if (topicId == null || topicId.isBlank()) {
            source.sendSuccess(() -> Component.translatable("command.exworld.help.header"), false);
            source.sendSuccess(() -> Component.translatable("command.exworld.help.topics", String.join(", ", ExworldHelpCatalog.ids())), false);
            return 1;
        }
        if ("all".equals(topicId)) {
            int count = 0;
            for (ExworldHelpCatalog.Topic topic : ExworldHelpCatalog.topics()) count += send(source, topic);
            return count;
        }
        ExworldHelpCatalog.Topic topic = ExworldHelpCatalog.topic(topicId);
        if (topic == null) {
            source.sendFailure(Component.translatable("command.exworld.help.unknown", topicId));
            return 0;
        }
        return send(source, topic);
    }

    private static int send(CommandSourceStack source, ExworldHelpCatalog.Topic topic) {
        source.sendSuccess(() -> Component.literal("—— " + topic.title() + " ——").withStyle(ChatFormatting.GOLD), false);
        for (ExworldHelpCatalog.Line line : topic.lines()) {
            source.sendSuccess(() -> Component.literal(line.usage()).withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("  " + line.detail()).withStyle(ChatFormatting.GRAY)), false);
        }
        return topic.lines().size();
    }
}
