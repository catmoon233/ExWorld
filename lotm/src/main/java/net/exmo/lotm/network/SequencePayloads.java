package net.exmo.lotm.network;

import net.exmo.lotm.Lotm;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class SequencePayloads {
    private SequencePayloads() {}

    public record OpenSequencePayload() implements CustomPacketPayload {
        public static final Type<OpenSequencePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Lotm.MODID, "open_sequence"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSequencePayload> STREAM_CODEC = StreamCodec.unit(new OpenSequencePayload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SkillView(String id, String kind, String nameKey, String descriptionKey, String spellId, String iconItem) {}

    public record EntryView(String id, String nameKey, String rankKey, String introductionKey, List<SkillView> skills) {}

    public record OwnedSpell(String spellId, int level) {}

    public record SequenceSnapshotPayload(
            boolean open,
            boolean hasSequence,
            String pathwayKey,
            String currentId,
            String currentNameKey,
            String currentRankKey,
            List<EntryView> entries,
            List<OwnedSpell> ownedSpells
    ) implements CustomPacketPayload {
        public static final Type<SequenceSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Lotm.MODID, "sequence_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SequenceSnapshotPayload> STREAM_CODEC = StreamCodec.of(
                SequenceSnapshotPayload::write, SequenceSnapshotPayload::read);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        private static void write(RegistryFriendlyByteBuf buf, SequenceSnapshotPayload payload) {
            buf.writeBoolean(payload.open);
            buf.writeBoolean(payload.hasSequence);
            buf.writeUtf(payload.pathwayKey);
            buf.writeUtf(payload.currentId);
            buf.writeUtf(payload.currentNameKey);
            buf.writeUtf(payload.currentRankKey);
            buf.writeVarInt(payload.entries.size());
            for (EntryView entry : payload.entries) {
                buf.writeUtf(entry.id);
                buf.writeUtf(entry.nameKey);
                buf.writeUtf(entry.rankKey);
                buf.writeUtf(entry.introductionKey);
                buf.writeVarInt(entry.skills.size());
                for (SkillView skill : entry.skills) writeSkill(buf, skill);
            }
            buf.writeVarInt(payload.ownedSpells.size());
            for (OwnedSpell spell : payload.ownedSpells) {
                buf.writeUtf(spell.spellId);
                buf.writeVarInt(spell.level);
            }
        }

        private static SequenceSnapshotPayload read(RegistryFriendlyByteBuf buf) {
            boolean open = buf.readBoolean();
            boolean hasSequence = buf.readBoolean();
            String pathwayKey = buf.readUtf();
            String currentId = buf.readUtf();
            String currentNameKey = buf.readUtf();
            String currentRankKey = buf.readUtf();
            int entryCount = buf.readVarInt();
            List<EntryView> entries = new ArrayList<>(entryCount);
            for (int i = 0; i < entryCount; i++) {
                String id = buf.readUtf();
                String nameKey = buf.readUtf();
                String rankKey = buf.readUtf();
                String introductionKey = buf.readUtf();
                int skillCount = buf.readVarInt();
                List<SkillView> skills = new ArrayList<>(skillCount);
                for (int s = 0; s < skillCount; s++) skills.add(readSkill(buf));
                entries.add(new EntryView(id, nameKey, rankKey, introductionKey, skills));
            }
            int spellCount = buf.readVarInt();
            List<OwnedSpell> owned = new ArrayList<>(spellCount);
            for (int i = 0; i < spellCount; i++) owned.add(new OwnedSpell(buf.readUtf(), buf.readVarInt()));
            return new SequenceSnapshotPayload(open, hasSequence, pathwayKey, currentId, currentNameKey, currentRankKey, entries, owned);
        }

        private static void writeSkill(RegistryFriendlyByteBuf buf, SkillView skill) {
            buf.writeUtf(skill.id);
            buf.writeUtf(skill.kind);
            buf.writeUtf(skill.nameKey);
            buf.writeUtf(skill.descriptionKey);
            buf.writeUtf(skill.spellId);
            buf.writeUtf(skill.iconItem);
        }

        private static SkillView readSkill(RegistryFriendlyByteBuf buf) {
            return new SkillView(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
        }
    }
}
