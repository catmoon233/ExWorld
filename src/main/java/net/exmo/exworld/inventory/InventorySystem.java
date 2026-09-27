package net.exmo.exworld.inventory;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class InventorySystem {
    private InventorySystem() {}

    public static void registerEvents() {
        NeoForge.EVENT_BUS.register(InventorySystem.class);
        NeoForge.EVENT_BUS.addListener(net.exmo.exworld.character.CharacterCommands::register);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("exworld")
                .then(Commands.literal("inventory")
                        .then(Commands.literal("edit").requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                                        context.getSource().sendFailure(Component.translatable("command.exworld.inventory_player"));
                                        return 0;
                                    }
                                    InventoryNetwork.sendEditor(player);
                                    return 1;
                                }))));
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerBackpack.of(player).grantDefaultCore();
        FootprintRuleStore.sync(player);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        PlayerBackpack.of(event.getEntity()).copyFrom(event.getOriginal());
        if (!event.getOriginal().level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            PlayerBackpack.of(event.getEntity()).dropExtras(false);
        }
    }

    @SubscribeEvent
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        for (ItemStack stack : PlayerBackpack.of(player).dropExtras(false)) {
            if (stack.isEmpty()) continue;
            ItemEntity entity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
            event.getDrops().add(entity);
        }
    }
}
