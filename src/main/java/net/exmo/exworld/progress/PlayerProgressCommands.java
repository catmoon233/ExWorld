package net.exmo.exworld.progress;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.*;
import com.mojang.brigadier.context.CommandContext;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.exmo.exworld.command.TokenArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Administrative content operation surface plus small player-readable mail fallbacks. */
public final class PlayerProgressCommands {
    private PlayerProgressCommands() {}
    public static void register() { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(PlayerProgressCommands.class); }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        var root=Commands.literal("exworld");
        root.then(resource()); root.then(gold()); root.then(quest()); root.then(mail()); event.getDispatcher().register(root);
    }
    /** Convenience command for the phone wallet: /exworld gold add <targets> <amount>. */
    private static LiteralArgumentBuilder<CommandSourceStack> gold() {
        return Commands.literal("gold").requires(s -> s.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(c -> {
                                            long amount = LongArgumentType.getLong(c, "amount");
                                            int count = 0;
                                            for (ServerPlayer player : EntityArgument.getPlayers(c, "targets")) {
                                                long balance = PlayerProgressSystem.vault().add(player.getServer(), player.getUUID(), PlayerResourceVault.GOLD, amount);
                                                player.sendSystemMessage(Component.literal("金币 +" + amount + "，当前余额：" + balance));
                                                count++;
                                            }
                                            int affected = count;
                                            c.getSource().sendSuccess(() -> Component.literal("已为 " + affected + " 名玩家增加金币"), true);
                                            return count;
                                        }))));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> resource() { return Commands.literal("resource").requires(s->s.hasPermission(2))
        .then(Commands.literal("get").then(Commands.argument("targets",EntityArgument.players()).then(Commands.argument("resource",TokenArgument.token()).executes(c->resource(c,"get",0)))))
        .then(Commands.literal("add").then(Commands.argument("targets",EntityArgument.players()).then(Commands.argument("resource",TokenArgument.token()).then(Commands.argument("amount",LongArgumentType.longArg(0)).executes(c->resource(c,"add",LongArgumentType.getLong(c,"amount")))))))
        .then(Commands.literal("take").then(Commands.argument("targets",EntityArgument.players()).then(Commands.argument("resource",TokenArgument.token()).then(Commands.argument("amount",LongArgumentType.longArg(0)).executes(c->resource(c,"take",LongArgumentType.getLong(c,"amount")))))))
        .then(Commands.literal("set").then(Commands.argument("targets",EntityArgument.players()).then(Commands.argument("resource",TokenArgument.token()).then(Commands.argument("amount",LongArgumentType.longArg(0)).executes(c->resource(c,"set",LongArgumentType.getLong(c,"amount"))))))); }
    private static int resource(CommandContext<CommandSourceStack> c,String action,long amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException { ResourceLocation id=ResourceLocation.parse(StringArgumentType.getString(c,"resource"));int count=0;for(ServerPlayer p:EntityArgument.getPlayers(c,"targets")){var vault=PlayerProgressSystem.vault();if(action.equals("add"))vault.add(p.getServer(),p.getUUID(),id,amount);else if(action.equals("take")){if(!vault.take(p.getServer(),p.getUUID(),id,amount))continue;}else if(action.equals("set"))vault.set(p.getServer(),p.getUUID(),id,amount);p.sendSystemMessage(Component.literal(id+": "+vault.balance(p.getServer(),p.getUUID(),id)));count++;}return count; }
    private static LiteralArgumentBuilder<CommandSourceStack> quest(){return Commands.literal("quest").requires(s->s.hasPermission(2))
        .then(Commands.literal("grant").then(targetQuest(c->PlayerProgressSystem.quests().grant(c.player(),c.id(),"admin"))))
        .then(Commands.literal("revoke").then(targetQuest(c->PlayerProgressSystem.quests().revoke(c.player(),c.id(),"admin"))))
        .then(Commands.literal("reset").then(targetQuest(c->PlayerProgressSystem.quests().reset(c.player(),c.id(),"admin"))))
        .then(Commands.literal("fail").then(targetQuest(c->PlayerProgressSystem.quests().fail(c.player(),c.id(),"admin"))))
        .then(Commands.literal("advance").then(targetQuest(c->PlayerProgressSystem.quests().advance(c.player(),c.id(),"","admin"))))
        .then(Commands.literal("branch").then(Commands.argument("targets",EntityArgument.players()).then(Commands.argument("quest",TokenArgument.token()).then(Commands.argument("node",TokenArgument.token()).executes(c->{ResourceLocation id=ResourceLocation.parse(StringArgumentType.getString(c,"quest"));int n=0;for(ServerPlayer p:EntityArgument.getPlayers(c,"targets"))if(PlayerProgressSystem.quests().chooseBranch(p,id,StringArgumentType.getString(c,"node")))n++;return n;})))))
        .then(Commands.literal("status").then(Commands.argument("targets",EntityArgument.players()).executes(c->{int n=0;for(ServerPlayer p:EntityArgument.getPlayers(c,"targets")){p.sendSystemMessage(Component.literal(PlayerProgressSystem.quests().snapshot(p).toString()));n++;}return n;})));}
    private interface QuestAction { boolean apply(QuestTarget target); } private record QuestTarget(ServerPlayer player,ResourceLocation id){}
    private static RequiredArgumentBuilder<CommandSourceStack,?> targetQuest(QuestAction action){return Commands.argument("targets",EntityArgument.players()).then(Commands.argument("quest",TokenArgument.token()).executes(c->{ResourceLocation id=ResourceLocation.parse(StringArgumentType.getString(c,"quest"));int n=0;for(ServerPlayer p:EntityArgument.getPlayers(c,"targets"))if(action.apply(new QuestTarget(p,id)))n++;return n;}));}
    private static LiteralArgumentBuilder<CommandSourceStack> mail(){return Commands.literal("mail")
        .then(Commands.literal("list").executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();p.sendSystemMessage(Component.literal(PlayerProgressSystem.quests().mailbox(p).stream().map(m->m.id()+" "+m.subject()+" ×"+m.attachments().size()).reduce("",(a,b)->a+"\n"+b)));return 1;}))
        .then(Commands.literal("claim").then(Commands.argument("id",TokenArgument.token()).executes(c->{ServerPlayer p=c.getSource().getPlayerOrException();return PlayerProgressSystem.rewards().claim(p,UUID.fromString(StringArgumentType.getString(c,"id")))?1:0;})))
        .then(Commands.literal("inspect").requires(s->s.hasPermission(2)).then(Commands.argument("target",GameProfileArgument.gameProfile()).executes(c->{var profile=GameProfileArgument.getGameProfiles(c,"target").iterator().next();ServerPlayer p=c.getSource().getServer().getPlayerList().getPlayer(profile.getId());if(p==null)return 0;c.getSource().sendSuccess(()->Component.literal(PlayerProgressSystem.quests().mailbox(p).toString()),false);return 1;})));}
}
