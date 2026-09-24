package net.exmo.exworld.npc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.npc.data.NpcCatalog;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.NpcPlace;
import net.exmo.exworld.npc.entity.UrbanNpc;
import net.exmo.exworld.npc.item.NpcWandItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class NpcCommands {
    private static final SuggestionProvider<CommandSourceStack> IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(NpcCatalog.get(context.getSource().getServer()).ids(), builder);

    private NpcCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var npc = Commands.literal("npc").requires(source -> source.hasPermission(2));
        event.getDispatcher().register(Commands.literal("exworld").then(npc
                .then(Commands.literal("create").then(Commands.argument("id", StringArgumentType.word()).executes(context -> {
                    String id = StringArgumentType.getString(context, "id");
                    if (!id.matches("[A-Za-z0-9_.:-]{1,64}")) {
                        context.getSource().sendFailure(Component.translatable("npc.exworld.bad_id"));
                        return 0;
                    }
                    NpcCatalog catalog = NpcCatalog.get(context.getSource().getServer());
                    if (catalog.document(id).isPresent()) {
                        context.getSource().sendFailure(Component.translatable("npc.exworld.exists"));
                        return 0;
                    }
                    catalog.put(NpcSystem.blank(id), java.util.List.of());
                    context.getSource().sendSuccess(() -> Component.translatable("npc.exworld.created", id), true);
                    return 1;
                })))
                .then(Commands.literal("spawn").then(Commands.argument("id", StringArgumentType.word()).suggests(IDS).executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;
                    String id = StringArgumentType.getString(context, "id");
                    UrbanNpc spawned = NpcSystem.spawn(player, id);
                    if (spawned == null) {
                        context.getSource().sendFailure(Component.translatable("npc.exworld.missing"));
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> Component.translatable("npc.exworld.spawned", id), true);
                    return 1;
                })))
                .then(Commands.literal("edit").then(Commands.argument("id", StringArgumentType.word()).suggests(IDS).executes(context -> open(context, false))))
                .then(Commands.literal("blueprint").then(Commands.argument("id", StringArgumentType.word()).suggests(IDS).executes(context -> open(context, true))))
                .then(Commands.literal("goto").then(Commands.argument("id", StringArgumentType.word()).suggests(IDS).executes(NpcCommands::go)))
                 .then(Commands.literal("copy").then(Commands.argument("from", StringArgumentType.word()).suggests(IDS)
                         .then(Commands.argument("to", StringArgumentType.word()).executes(NpcCommands::copy))))
                 .then(Commands.literal("despawn").then(Commands.argument("id", StringArgumentType.word()).suggests(IDS).executes(NpcCommands::despawn)))
                 .then(Commands.literal("remove").then(Commands.argument("id", StringArgumentType.word()).suggests(IDS).executes(context -> {
                     String id = StringArgumentType.getString(context, "id");
                     NpcCatalog catalog = NpcCatalog.get(context.getSource().getServer());
                     if (catalog.document(id).isEmpty()) {
                         context.getSource().sendFailure(Component.translatable("npc.exworld.missing"));
                         return 0;
                     }
                     int gone = NpcSystem.despawn(context.getSource().getServer(), id);
                     if (!catalog.remove(id)) {
                         context.getSource().sendFailure(Component.translatable("npc.exworld.missing"));
                         return 0;
                     }
                     context.getSource().sendSuccess(() -> Component.translatable("npc.exworld.removed", id), true);
                     if (gone > 0) context.getSource().sendSuccess(() -> Component.translatable("npc.exworld.despawned", gone), false);
                     return 1;
                 })))
                .then(Commands.literal("wand").executes(context -> give(context, ""))
                        .then(Commands.argument("id", StringArgumentType.word()).suggests(IDS).executes(context -> give(context, StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("list").executes(context -> {
                    String ids = String.join(", ", NpcCatalog.get(context.getSource().getServer()).ids());
                    context.getSource().sendSuccess(() -> Component.literal(ids.isBlank() ? "-" : ids), false);
                    return 1;
                }))));
    }

    private static int open(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, boolean blueprint) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;
        String id = StringArgumentType.getString(context, "id");
        if (blueprint) NpcSystem.openBlueprint(player, id);
        else NpcSystem.openEditor(player, id, "");
        return 1;
    }

    private static int go(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;
        NpcDocument doc = NpcCatalog.get(player.server).document(StringArgumentType.getString(context, "id")).orElse(null);
        if (doc == null) {
            context.getSource().sendFailure(Component.translatable("npc.exworld.missing"));
            return 0;
        }
        NpcPlace home = doc.place(doc.homePlaceId()).orElse(null);
        if (home == null) {
            context.getSource().sendFailure(Component.translatable("npc.exworld.missing"));
            return 0;
        }
        if (!home.dimension().equals(player.serverLevel().dimension().location().toString())) {
            context.getSource().sendFailure(Component.translatable("npc.exworld.other_dimension"));
            return 0;
        }
        player.teleportTo(home.x(), home.y(), home.z());
        context.getSource().sendSuccess(() -> Component.translatable("npc.exworld.gone", home.id()), false);
        return 1;
    }

    private static int give(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, String id) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) return 0;
        if (!id.isBlank() && NpcCatalog.get(player.server).document(id).isEmpty()) {
            context.getSource().sendFailure(Component.translatable("npc.exworld.missing"));
            return 0;
        }
        ItemStack stack = new ItemStack(ExWorldContent.NPC_WAND.get());
        if (!id.isBlank()) NpcWandItem.remember(stack, id);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        context.getSource().sendSuccess(() -> Component.translatable("npc.exworld.gave_wand"), true);
        return 1;
    }
 
     private static int copy(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
         String from = StringArgumentType.getString(context, "from");
         String to = StringArgumentType.getString(context, "to");
         if (!to.matches("[A-Za-z0-9_.:-]{1,64}")) {
             context.getSource().sendFailure(Component.translatable("npc.exworld.bad_id"));
             return 0;
         }
         NpcCatalog catalog = NpcCatalog.get(context.getSource().getServer());
         NpcDocument source = catalog.document(from).orElse(null);
         if (source == null) {
             context.getSource().sendFailure(Component.translatable("npc.exworld.missing"));
             return 0;
         }
         if (catalog.document(to).isPresent() || from.equals(to)) {
             context.getSource().sendFailure(Component.translatable("npc.exworld.exists"));
             return 0;
         }
         catalog.put(source.withId(to, false), NpcSystem.retarget(catalog.outgoing(from), to));
         context.getSource().sendSuccess(() -> Component.translatable("npc.exworld.copied", to), true);
         return 1;
     }
 
     private static int despawn(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
         String id = StringArgumentType.getString(context, "id");
         if (NpcCatalog.get(context.getSource().getServer()).document(id).isEmpty()) {
             context.getSource().sendFailure(Component.translatable("npc.exworld.missing"));
             return 0;
         }
         int gone = NpcSystem.despawn(context.getSource().getServer(), id);
         context.getSource().sendSuccess(() -> Component.translatable("npc.exworld.despawned", gone), true);
         return 1;
     }
}
