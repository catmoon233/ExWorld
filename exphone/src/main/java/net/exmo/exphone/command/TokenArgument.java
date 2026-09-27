package net.exmo.exphone.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.List;
import net.exmo.exphone.ExPhone;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * One command token, including {@code :} and {@code /}.
 * {@code StringArgumentType.word()} stops at those characters.
 */
public final class TokenArgument implements ArgumentType<String> {
    private static boolean registered;

    private TokenArgument() {}

    public static TokenArgument token() {
        return new TokenArgument();
    }

    public static void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        DeferredRegister<ArgumentTypeInfo<?, ?>> types =
                DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, ExPhone.MODID);
        types.register("token", () -> ArgumentTypeInfos.registerByClass(
                TokenArgument.class, SingletonArgumentInfo.contextFree(TokenArgument::token)));
        types.register(bus);
    }

    public static String get(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead() || Character.isWhitespace(reader.peek())) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedStartOfQuote().createWithContext(reader);
        }
        if (StringReader.isQuotedStringStart(reader.peek())) return reader.readQuotedString();
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) reader.skip();
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public Collection<String> getExamples() {
        return List.of("exphone:gold", "other:credits");
    }
}
