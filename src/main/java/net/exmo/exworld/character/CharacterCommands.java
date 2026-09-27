package net.exmo.exworld.character;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.exmo.exworld.network.CharacterPayloads;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class CharacterCommands {
    private CharacterCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("exworld").then(Commands.literal("character")
                .then(Commands.literal("import").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("file", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> suggest(builder))
                                        .executes(context -> importFile(
                                                EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "file"),
                                                context.getSource())))))
                .then(Commands.literal("clear").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(context -> {
                            int count = 0;
                            for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) {
                                CharacterNetwork.setText(player, "");
                                count++;
                                context.getSource().sendSuccess(() -> Component.translatable(
                                        "command.exworld.character.cleared", player.getDisplayName()), true);
                            }
                            return count;
                        })))));
    }

    private static int importFile(Iterable<ServerPlayer> players, String raw,
                                  net.minecraft.commands.CommandSourceStack source) {
        Path path = resolve(raw);
        if (path == null) {
            source.sendFailure(Component.translatable("command.exworld.character.missing", raw));
            return 0;
        }
        String text;
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length > CharacterPayloads.MAX_BYTES) {
                source.sendFailure(Component.translatable("command.exworld.character.too_large", path.getFileName().toString()));
                return 0;
            }
            text = new String(bytes, StandardCharsets.UTF_8);
            if (text.startsWith("\uFEFF")) text = text.substring(1);
        } catch (IOException exception) {
            source.sendFailure(Component.translatable("command.exworld.character.read_failed", path.getFileName().toString()));
            return 0;
        }
        int count = 0;
        String imported = text;
        String fileName = path.getFileName().toString();
        for (ServerPlayer player : players) {
            CharacterNetwork.setText(player, imported);
            source.sendSuccess(() -> Component.translatable(
                    "command.exworld.character.imported", player.getDisplayName(), fileName), true);
            count++;
        }
        return count;
    }

    static Path resolve(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("..")) return null;
        String name = raw.trim().replace('\\', '/');
        Path game = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path[] candidates = Path.of(name).isAbsolute()
                ? new Path[] { Path.of(name) }
                : new Path[] {
                        game.resolve(name),
                        game.resolve("characters").resolve(name),
                        game.resolve("config").resolve("exworld").resolve("characters").resolve(name)
                };
        for (Path candidate : candidates) {
            Path path = candidate.toAbsolutePath().normalize();
            if (!path.startsWith(game)) continue;
            String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!fileName.endsWith(".md") || !Files.isRegularFile(path)) continue;
            return path;
        }
        return null;
    }

    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder) {
        Path game = FMLPaths.GAMEDIR.get();
        suggestDir(builder, game.resolve("characters"), "characters");
        suggestDir(builder, game.resolve("config").resolve("exworld").resolve("characters"), "config/exworld/characters");
        return builder.buildFuture();
    }

    private static void suggestDir(SuggestionsBuilder builder, Path dir, String prefix) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .map(path -> prefix + "/" + path.getFileName())
                    .forEach(builder::suggest);
        } catch (IOException ignored) {
        }
    }
}
