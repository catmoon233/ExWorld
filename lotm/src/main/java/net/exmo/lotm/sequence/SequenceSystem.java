package net.exmo.lotm.sequence;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SequenceSystem {
    private SequenceSystem() {}

    public static void register(IEventBus modBus) {
        SequenceAttachments.register(modBus);
        modBus.addListener(SequenceSystem::commonSetup);
        modBus.addListener(SequenceSystem::registerPayloads);
    }

    public static void registerEvents() {
        NeoForge.EVENT_BUS.register(SequenceCommands.class);
        NeoForge.EVENT_BUS.register(PassiveDispatcher.class);
        NeoForge.EVENT_BUS.register(SequenceSpellOwnership.class);
        NeoForge.EVENT_BUS.addListener(SequenceSystem::login);
        NeoForge.EVENT_BUS.addListener(SequenceSystem::copied);
        NeoForge.EVENT_BUS.addListener(SequenceSystem::respawn);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> NeoForge.EVENT_BUS.post(new RegisterSequencesEvent()));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        SequenceNetwork.register(event.registrar("1"));
    }

    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SequenceService.reapply(player);
        SequenceNetwork.sync(player, false);
    }
    private static void copied(PlayerEvent.Clone event) {
        PlayerSequenceData from = event.getOriginal().getData(SequenceAttachments.SEQUENCE.get());
        PlayerSequenceData to = event.getEntity().getData(SequenceAttachments.SEQUENCE.get());
        to.copyFrom(from);
        if (event.getEntity() instanceof ServerPlayer player) SequenceService.reapply(player);
    }

    private static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SequenceService.reapply(player);
            SequenceNetwork.sync(player, false);
        }
    }
}
