package net.exmo.exworld.monster;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

/** Administrator-facing, world-scoped package operations. File arguments are names only, never paths. */
public final class MonsterCommands {
    private MonsterCommands() {}
    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        var monsters = Commands.literal("monsters");
        monsters.then(Commands.literal("packages").executes(context -> list(context.getSource())));
        monsters.then(Commands.literal("reload").executes(context -> reload(context.getSource())));
        monsters.then(Commands.literal("edit").then(Commands.argument("package", StringArgumentType.word()).executes(context -> edit(context.getSource(), StringArgumentType.getString(context, "package")))));
        monsters.then(Commands.literal("enable").then(Commands.argument("package", StringArgumentType.word()).executes(context -> enabled(context.getSource(), StringArgumentType.getString(context, "package"), true))));
        monsters.then(Commands.literal("disable").then(Commands.argument("package", StringArgumentType.word()).executes(context -> enabled(context.getSource(), StringArgumentType.getString(context, "package"), false))));
        monsters.then(Commands.literal("move").then(Commands.argument("package", StringArgumentType.word()).then(Commands.argument("index", IntegerArgumentType.integer(0)).executes(context -> move(context.getSource(), StringArgumentType.getString(context, "package"), IntegerArgumentType.getInteger(context, "index"))))));
        monsters.then(Commands.literal("import").then(Commands.argument("file", StringArgumentType.word()).executes(context -> importPackage(context.getSource(), StringArgumentType.getString(context, "file")))));
        monsters.then(Commands.literal("export").then(Commands.argument("package", StringArgumentType.word()).executes(context -> exportPackage(context.getSource(), StringArgumentType.getString(context, "package")))));
        dispatcher.register(Commands.literal("exworld").requires(source -> source.hasPermission(2)).then(monsters));
    }
    private static MonsterProfileRegistry registry(CommandSourceStack source) { return MonsterProfileRegistry.active().orElseThrow(() -> new IllegalStateException("monster registry is not ready")); }
    private static int list(CommandSourceStack source) { MonsterProfileRegistry registry=registry(source);source.sendSuccess(()->Component.literal("怪物数据包："+String.join("、",registry.enabled())),false);for(var pack:registry.packages())source.sendSuccess(()->Component.literal(" - "+pack.id()+"  "+pack.displayName()+" "+pack.version()+(pack.valid()?"":" 错误："+pack.error())),false);if(source.getEntity() instanceof ServerPlayer player)net.exmo.exworld.network.MonsterNetwork.sendPackages(player);return 1; }
    private static int reload(CommandSourceStack source){boolean ok=registry(source).reload();source.sendSuccess(()->Component.literal(ok?"怪物数据包已热重载":"热重载失败，已保留上一份有效配置"),false);return ok?1:0;}
    private static int enabled(CommandSourceStack source,String id,boolean enabled){boolean ok=registry(source).setEnabled(id,enabled);source.sendSuccess(()->Component.literal(ok?(enabled?"已启用：":"已停用：")+id:"操作失败，已保留上一份有效配置"),false);return ok?1:0;}
    private static int move(CommandSourceStack source,String id,int index){boolean ok=registry(source).reorder(id,index);source.sendSuccess(()->Component.literal(ok?"已调整优先级："+id:"未知数据包或热重载失败"),false);return ok?1:0;}
    private static int importPackage(CommandSourceStack source,String file){try{registry(source).manager().importPackage(file);source.sendSuccess(()->Component.literal("已导入 "+file+"，请在编辑器中启用它。"),false);return 1;}catch(IOException|IllegalArgumentException error){source.sendFailure(Component.literal("导入失败："+error.getMessage()));return 0;}}
    private static int exportPackage(CommandSourceStack source,String id){try{var file=registry(source).manager().exportPackage(id);source.sendSuccess(()->Component.literal("已导出到 "+file.getFileName()),false);return 1;}catch(IOException|IllegalArgumentException error){source.sendFailure(Component.literal("导出失败："+error.getMessage()));return 0;}}
    private static int edit(CommandSourceStack source,String id){
        try { var pack=registry(source).manager().load(id); ServerPlayer player=source.getPlayer(); if(player != null) net.exmo.exworld.network.MonsterNetwork.sendEditor(player,pack); else source.sendSuccess(()->Component.literal("数据包 "+id+" 包含 "+pack.templates().size()+" 个模板和 "+pack.rules().size()+" 条规则。"),false); return 1; }
        catch(IOException|IllegalArgumentException error){source.sendFailure(Component.literal("无法打开数据包："+error.getMessage()));return 0;}
    }
}
