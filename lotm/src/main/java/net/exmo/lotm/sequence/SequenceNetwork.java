package net.exmo.lotm.sequence;

import net.exmo.lotm.network.SequencePayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public final class SequenceNetwork {
    private SequenceNetwork() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(SequencePayloads.OpenSequencePayload.TYPE, SequencePayloads.OpenSequencePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) sync(player, true);
                });
        registrar.playToClient(SequencePayloads.SequenceSnapshotPayload.TYPE, SequencePayloads.SequenceSnapshotPayload.STREAM_CODEC,
                (payload, context) -> SequenceClientHooks.receive(payload));
    }

    public static void sync(ServerPlayer player, boolean open) {
        PacketDistributor.sendToPlayer(player, snapshot(player, open));
    }

    public static SequencePayloads.SequenceSnapshotPayload snapshot(ServerPlayer player, boolean open) {
        SequenceDefinition current = SequenceService.current(player).orElse(null);
        PathwayDefinition pathway = SequenceService.pathway(player).orElse(null);
        List<SequencePayloads.EntryView> entries = new ArrayList<>();
        for (SequenceDefinition sequence : SequenceService.unlocked(player)) {
            entries.add(new SequencePayloads.EntryView(
                    sequence.id().toString(),
                    sequence.nameKey(),
                    sequence.rank().translationKey(),
                    sequence.introductionKey(),
                    sequence.skills().stream().map(SequenceNetwork::skill).toList()));
        }
        List<SequencePayloads.OwnedSpell> owned = SequenceService.ownedSpells(player).stream()
                .map(skill -> new SequencePayloads.OwnedSpell(skill.ref().toString(), skill.level()))
                .toList();
        return new SequencePayloads.SequenceSnapshotPayload(
                open,
                current != null,
                pathway == null ? "" : pathway.nameKey(),
                current == null ? "" : current.id().toString(),
                current == null ? "" : current.nameKey(),
                current == null ? "" : current.rank().translationKey(),
                entries,
                owned);
    }

    private static SequencePayloads.SkillView skill(SequenceSkill skill) {
        String spellId = skill.kind() == SkillKind.ACTIVE ? skill.ref().toString() : "";
        String icon = skill.iconItem();
        if (icon.isBlank() && skill.kind() == SkillKind.PASSIVE) {
            icon = PassiveRegistry.get(skill.ref()).map(PassiveDefinition::iconItem).orElse("minecraft:iron_sword");
        }
        return new SequencePayloads.SkillView(
                skill.ref().toString(),
                skill.kind().token(),
                skill.nameKey(),
                skill.descriptionKey(),
                spellId,
                icon);
    }

    public static Component name(SequenceDefinition sequence) {
        return sequence == null ? Component.translatable("command.exworld.sequence.none") : Component.translatable(sequence.nameKey());
    }
}
