package net.exmo.lotm.sequence;

import net.exmo.exworld.Exworld;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class SequenceAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Exworld.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerSequenceData>> SEQUENCE =
            ATTACHMENTS.register("sequence", () -> AttachmentType.serializable(PlayerSequenceData::new).copyOnDeath().build());

    private SequenceAttachments() {}

    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }
}
