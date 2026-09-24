package net.exmo.exworld.social;

import net.exmo.exworld.Config;
import net.exmo.exworld.network.WorldNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.ServerChatEvent;

/** Rewrites adventure and spectator chat, and opens the range configuration screen. */
public final class NearbyChat {
    private NearbyChat() {}

    public static int open(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        if (!canConfigure(player)) {
            source.sendFailure(Component.translatable("screen.exworld.nearby_denied"));
            return 0;
        }
        WorldNetwork.sendNearbyChatOpen(player, Config.adventureChatDistance, Config.adventureChatRange);
        return 1;
    }

    public static void save(ServerPlayer player, int distance, int range) {
        if (!canConfigure(player)) {
            player.sendSystemMessage(Component.translatable("screen.exworld.nearby_denied"));
            return;
        }
        int savedDistance = clamp(distance, 1, 1024);
        int savedRange = clamp(range, 1, 384);
        Config.setAdventureChat(savedDistance, savedRange);
        player.sendSystemMessage(Component.translatable("screen.exworld.nearby_saved", savedDistance, savedRange));
    }

    public static void deliver(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        NearbyChatRules.Audience speaker = audience(sender);
        if (!NearbyChatRules.rewrites(speaker)) return;
        event.setCanceled(true);
        Component body = event.getMessage();
        if (body == null || body.getString().isBlank()) body = Component.literal(event.getRawText());
        Component line = Component.translatable("chat.type.text", sender.getDisplayName(), body);
        double distance = Config.adventureChatDistance;
        double range = Config.adventureChatRange;
        for (ServerPlayer listener : sender.server.getPlayerList().getPlayers()) {
            boolean sameDimension = listener.level() == sender.level();
            if (NearbyChatRules.hears(audience(listener), speaker, sameDimension,
                    listener.getX() - sender.getX(), listener.getY() - sender.getY(), listener.getZ() - sender.getZ(),
                    distance, range)) {
                listener.sendSystemMessage(line);
            }
        }
    }

    public static boolean canConfigure(ServerPlayer player) {
        return player.isCreative() || player.hasPermissions(2);
    }

    private static NearbyChatRules.Audience audience(ServerPlayer player) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        return NearbyChatRules.audience(mode == GameType.SPECTATOR, mode == GameType.ADVENTURE);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
