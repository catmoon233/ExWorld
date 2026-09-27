package net.exmo.lotm.story;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public final class StoryActions {
    public static final int LETTER_LIMIT = 10;
    public static final int BREAD_PRICE = 3;
    public static final int BREAD_COUNT = 2;
    public static final int REMEDY_PRICE = 6;

    private StoryActions() {}

    public static void openLetter(ServerPlayer player) {
        StoryData data = StoryData.get(player);
        if (net.exmo.exworld.mystery.MysteryGame.active(player))
            data.prepareLetters(net.exmo.exworld.mystery.MysteryGame.currentRun(player));
        PacketDistributor.sendToPlayer(player, new StoryPayloads.OpenLetterPayload(data.letterFor(player.getUUID())));
    }

    public static void sendLetter(ServerPlayer player, String raw) {
        String text = sanitize(raw);
        if (text.isEmpty()) {
            tell(player, "item.lotm.message_token.empty");
            return;
        }
        if (text.codePointCount(0, text.length()) > LETTER_LIMIT) {
            tell(player, "item.lotm.message_token.too_long");
            return;
        }
        if (net.exmo.exworld.mystery.MysteryGame.active(player)) {
            StoryData data = StoryData.get(player);
            data.prepareLetters(net.exmo.exworld.mystery.MysteryGame.currentRun(player));
            data.sendTo(player.getUUID(),
                    net.exmo.exworld.mystery.MysteryGame.counterpartIds(player), text);
        } else StoryData.get(player).send(player.getUUID(), text);
        tell(player, "item.lotm.message_token.sent");
        openLetter(player);
    }

    public static void buy(ServerPlayer player, String offer) {
        int price;
        ItemStack result;
        if ("bread".equals(offer)) {
            price = BREAD_PRICE;
            result = new ItemStack(Items.BREAD, BREAD_COUNT);
        } else if ("remedy".equals(offer)) {
            price = REMEDY_PRICE;
            result = new ItemStack(StoryItems.REMEDY.get());
        } else {
            return;
        }
        if (!player.getAbilities().instabuild && count(player, StoryItems.COIN.get()) < price) {
            tell(player, "item.lotm.coin.poor");
            return;
        }
        if (!player.getAbilities().instabuild) take(player, StoryItems.COIN.get(), price);
        if (!player.getInventory().add(result) && !result.isEmpty()) player.drop(result, false);
        tell(player, "item.lotm.coin.bought");
    }

    public static void anchor(ServerPlayer player) {
        StoryData data = StoryData.get(player);
        if (data.anchored() && !player.hasPermissions(2)) {
            tell(player, "item.lotm.sealed_key.locked");
            return;
        }
        data.anchor(player);
        tell(player, "item.lotm.sealed_key.anchored");
    }

    public static void recall(ServerPlayer player) {
        StoryData data = StoryData.get(player);
        ServerLevel destination = data.destination(player);
        if (destination == null) {
            tell(player, "item.lotm.sealed_key.no_train");
            return;
        }
        if (net.exmo.exworld.mystery.MysteryGame.active(player)) {
            List<ServerPlayer> group = net.exmo.exworld.mystery.MysteryGame.board(player);
            if (group.isEmpty()) { tell(player, "item.lotm.sealed_key.locked"); return; }
            int offset = 0;
            for (ServerPlayer member : group) {
                pull(member, destination, data.x + offset, data.y, data.z, data.yaw);
                offset++;
            }
            group.forEach(member -> tell(member, "item.lotm.sealed_key.recalled"));
            return;
        }
        ServerPlayer other = counterpart(player, data);
        pull(player, destination, data.x, data.y, data.z, data.yaw);
        if (other != null) {
            pull(other, destination, data.x + 1.0, data.y, data.z, data.yaw);
            tell(player, "item.lotm.sealed_key.recalled");
            tell(other, "item.lotm.sealed_key.recalled");
            return;
        }
        List<ServerPlayer> others = player.server.getPlayerList().getPlayers().stream().filter(online -> online != player).toList();
        tell(player, others.size() > 1 ? "item.lotm.sealed_key.crowded" : "item.lotm.sealed_key.missing");
    }

    public static void pair(ServerPlayer left, ServerPlayer right) {
        StoryData.get(left).pair(left.getUUID(), right.getUUID());
    }

    private static ServerPlayer counterpart(ServerPlayer player, StoryData data) {
        UUID paired = data.pairOf(player.getUUID());
        if (paired != null) {
            ServerPlayer other = player.server.getPlayerList().getPlayer(paired);
            if (other != null && other != player) return other;
        }
        List<ServerPlayer> others = player.server.getPlayerList().getPlayers().stream().filter(online -> online != player).toList();
        if (others.size() == 1) {
            data.pair(player.getUUID(), others.get(0).getUUID());
            return others.get(0);
        }
        List<ServerPlayer> holders = others.stream().filter(StoryActions::holdsKey).toList();
        if (holders.size() == 1) {
            data.pair(player.getUUID(), holders.get(0).getUUID());
            return holders.get(0);
        }
        return null;
    }

    private static boolean holdsKey(ServerPlayer player) {
        return player.getInventory().countItem(StoryItems.SEALED_KEY.get()) > 0;
    }

    private static void pull(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw) {
        player.teleportTo(level, x, y, z, yaw, player.getXRot());
        player.resetFallDistance();
        level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.15F);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 1.0, z, 12, 0.2, 0.4, 0.2, 0.02);
    }

    static String sanitize(String raw) {
        if (raw == null) return "";
        return raw.replace("\n", "").replace("\r", "").trim();
    }

    private static int count(Player player, Item item) {
        return player.getInventory().countItem(item);
    }

    private static void take(Player player, Item item, int count) {
        int left = count;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) continue;
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
        }
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
