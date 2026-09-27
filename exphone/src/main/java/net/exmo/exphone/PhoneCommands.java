package net.exmo.exphone;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import net.exmo.exphone.api.PhoneEconomy;
import net.exmo.exphone.command.TokenArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class PhoneCommands {
    private PhoneCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("exphone")
                .then(Commands.literal("sim").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("number", TokenArgument.token())
                                .executes(context -> giveSim(context.getSource(), TokenArgument.get(context, "number"), null))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> giveSim(context.getSource(),
                                                TokenArgument.get(context, "number"),
                                                EntityArgument.getPlayer(context, "target"))))))
                .then(Commands.literal("accept").executes(context -> accept(context.getSource())))
                .then(Commands.literal("hangup").executes(context -> hangup(context.getSource())))
                .then(gold())
                .then(money()));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> gold() {
        return Commands.literal("gold").requires(source -> source.hasPermission(2))
                .then(Commands.literal("get").then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> money(context.getSource(), PhoneEconomy.GOLD, "get", 0, EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("add").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                .executes(context -> money(context.getSource(), PhoneEconomy.GOLD, "add",
                                        LongArgumentType.getLong(context, "amount"), EntityArgument.getPlayers(context, "targets"))))))
                .then(Commands.literal("take").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                .executes(context -> money(context.getSource(), PhoneEconomy.GOLD, "take",
                                        LongArgumentType.getLong(context, "amount"), EntityArgument.getPlayers(context, "targets"))))))
                .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                .executes(context -> money(context.getSource(), PhoneEconomy.GOLD, "set",
                                        LongArgumentType.getLong(context, "amount"), EntityArgument.getPlayers(context, "targets"))))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> money() {
        return Commands.literal("money").requires(source -> source.hasPermission(2))
                .then(Commands.argument("currency", TokenArgument.token())
                        .then(Commands.literal("get").then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> money(context.getSource(), currency(context), "get", 0, EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("add").then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(context -> money(context.getSource(), currency(context), "add",
                                                LongArgumentType.getLong(context, "amount"), EntityArgument.getPlayers(context, "targets"))))))
                        .then(Commands.literal("take").then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(context -> money(context.getSource(), currency(context), "take",
                                                LongArgumentType.getLong(context, "amount"), EntityArgument.getPlayers(context, "targets"))))))
                        .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(context -> money(context.getSource(), currency(context), "set",
                                                LongArgumentType.getLong(context, "amount"), EntityArgument.getPlayers(context, "targets")))))));
    }

    private static ResourceLocation currency(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        String token = TokenArgument.get(context, "currency");
        return ResourceLocation.parse(token.indexOf(':') >= 0 ? token : ExPhone.MODID + ":" + token);
    }

    private static int money(CommandSourceStack source, ResourceLocation currency, String action, long amount, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            var server = player.server;
            if ("add".equals(action)) PhoneEconomy.add(server, player.getUUID(), currency, amount);
            else if ("take".equals(action) && !PhoneEconomy.take(server, player.getUUID(), currency, amount)) continue;
            else if ("set".equals(action)) PhoneEconomy.set(server, player.getUUID(), currency, amount);
            long balance = PhoneEconomy.balance(server, player.getUUID(), currency);
            player.sendSystemMessage(Component.literal(currency + "：" + balance));
            count++;
        }
        int affected = count;
        source.sendSuccess(() -> Component.literal(currency + " 已处理 " + affected + " 名玩家"), true);
        return count;
    }

    private static int accept(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal(PhoneCalls.accept(player)), false);
        return 1;
    }

    private static int hangup(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal(PhoneCalls.hangup(player)), false);
        return 1;
    }

    private static int giveSim(CommandSourceStack source, String number, ServerPlayer target) throws CommandSyntaxException {
        String digits = number.replaceAll("\\D", "");
        if (digits.length() != 7) {
            source.sendFailure(Component.translatable("command.exphone.sim.bad_number", number));
            return 0;
        }
        ServerPlayer recipient = target != null ? target : source.getPlayerOrException();
        PhoneData data = PhoneData.get(recipient);
        if (data.byNumber(digits) != null) {
            source.sendFailure(Component.translatable("command.exphone.sim.taken", digits));
            return 0;
        }
        ItemStack sim = new ItemStack(PhoneItems.SIM.get());
        PhoneStacks.setNumber(sim, digits);
        if (!recipient.getInventory().add(sim)) recipient.drop(sim, false);
        source.sendSuccess(() -> Component.translatable("command.exphone.sim.given", recipient.getDisplayName(), digits), true);
        return 1;
    }
}
