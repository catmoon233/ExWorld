package net.exmo.exworld.character;

import net.exmo.exworld.inventory.InventoryRegistries;
import net.exmo.exworld.network.CharacterPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CharacterNetwork {
    private CharacterNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(CharacterPayloads.OpenCharacterPayload.TYPE, CharacterPayloads.OpenCharacterPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) open(player);
                });
        registrar.playToClient(CharacterPayloads.CharacterIntroPayload.TYPE, CharacterPayloads.CharacterIntroPayload.STREAM_CODEC,
                (payload, context) -> CharacterClientHooks.open(payload.text()));
    }

    public static void open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CharacterPayloads.CharacterIntroPayload(text(player)));
    }

    public static String text(ServerPlayer player) {
        CharacterIntro intro = player.getData(InventoryRegistries.CHARACTER_INTRO.get());
        return intro == null || intro.text() == null ? "" : intro.text();
    }

    public static void setText(ServerPlayer player, String text) {
        CharacterIntro intro = player.getData(InventoryRegistries.CHARACTER_INTRO.get());
        intro.text(text);
        player.setData(InventoryRegistries.CHARACTER_INTRO.get(), intro);
    }
}
