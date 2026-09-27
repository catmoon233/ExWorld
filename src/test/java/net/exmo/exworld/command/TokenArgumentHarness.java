package net.exmo.exworld.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public final class TokenArgumentHarness {
    private TokenArgumentHarness() {}

    public static void main(String[] args) throws CommandSyntaxException {
        CommandDispatcher<Object> word = new CommandDispatcher<>();
        word.register(LiteralArgumentBuilder.<Object>literal("exworld")
                .then(LiteralArgumentBuilder.literal("npc")
                        .then(LiteralArgumentBuilder.literal("blueprint")
                                .then(RequiredArgumentBuilder.argument("id", StringArgumentType.word())
                                        .executes(context -> 1)))));
        boolean rejected = false;
        try {
            word.execute("exworld npc blueprint exworld:stall_vendor", new Object());
        } catch (CommandSyntaxException error) {
            rejected = error.getCursor() == "exworld npc blueprint exworld".length();
            if (!rejected) throw new IllegalStateException("unexpected word() cursor " + error.getCursor() + ": " + error.getMessage());
        }
        if (!rejected) throw new IllegalStateException("word() accepted a resource id");

        CommandDispatcher<Object> fixed = new CommandDispatcher<>();
        fixed.register(LiteralArgumentBuilder.<Object>literal("exworld")
                .then(LiteralArgumentBuilder.literal("npc")
                        .then(LiteralArgumentBuilder.literal("blueprint")
                                .then(RequiredArgumentBuilder.argument("id", TokenArgument.token())
                                        .executes(context -> expect(context, "id", "exworld:stall_vendor"))))
                        .then(LiteralArgumentBuilder.literal("copy")
                                .then(RequiredArgumentBuilder.argument("from", TokenArgument.token())
                                        .then(RequiredArgumentBuilder.argument("to", TokenArgument.token())
                                                .executes(context -> {
                                                    expect(context, "from", "exworld:a");
                                                    return expect(context, "to", "exworld:mystery/foo");
                                                }))))));
        check(fixed.execute("exworld npc blueprint exworld:stall_vendor", new Object()) == 1, "unquoted id");
        check(fixed.execute("exworld npc blueprint \"exworld:stall_vendor\"", new Object()) == 1, "quoted id");
        check(fixed.execute("exworld npc copy exworld:a exworld:mystery/foo", new Object()) == 1, "two ids");
        boolean missingSpace = false;
        try {
            fixed.execute("exworld npc copy exworld:aexworld:b", new Object());
        } catch (CommandSyntaxException error) {
            missingSpace = true;
        }
        check(missingSpace, "arguments still require a separating space");
        System.out.println("token argument accepts resource ids");
    }

    private static int expect(com.mojang.brigadier.context.CommandContext<Object> context, String name, String expected) {
        String actual = StringArgumentType.getString(context, name);
        if (!expected.equals(actual)) throw new IllegalStateException(name + "=" + actual);
        return 1;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
