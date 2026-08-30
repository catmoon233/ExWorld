package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.model.ManualChunkGroupLayout;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Complete editor draft. It is compressed and server-validated before it changes SavedData. */
public record SaveWorldGroupEditPayload(boolean manualGroups, long baseRevision,
                                        List<ManualChunkGroupLayout.Group> groups) implements CustomPacketPayload {
    private static final int MAX_GROUPS = 16_384;
    private static final int MAX_MEMBERS = 16_384;
    private static final int MAX_COMPRESSED_BYTES = 1_048_576;
    public static final Type<SaveWorldGroupEditPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "save_world_group_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaveWorldGroupEditPayload> STREAM_CODEC = StreamCodec.of(
            SaveWorldGroupEditPayload::encode, SaveWorldGroupEditPayload::decode);

    public SaveWorldGroupEditPayload { groups = List.copyOf(groups); }
    public SaveWorldGroupEditPayload(boolean manualGroups, List<ManualChunkGroupLayout.Group> groups) {
        this(manualGroups, 0L, groups);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SaveWorldGroupEditPayload payload) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(bytes))) {
                output.writeBoolean(payload.manualGroups);
                output.writeLong(payload.baseRevision);
                output.writeInt(payload.groups.size());
                for (ManualChunkGroupLayout.Group group : payload.groups) {
                    output.writeUTF(group.id()); output.writeUTF(group.name()); output.writeUTF(group.icon());
                    output.writeUTF(group.site()); output.writeUTF(group.resources()); output.writeBoolean(group.configured());
                    output.writeInt(group.tileIds().size());
                    for (String tileId : group.tileIds()) output.writeUTF(tileId);
                }
            }
            byte[] encoded = bytes.toByteArray();
            if (encoded.length > MAX_COMPRESSED_BYTES) throw new IllegalArgumentException("world group edit is too large");
            buffer.writeByteArray(encoded);
        } catch (IOException exception) {
            throw new IllegalStateException("could not encode world group edit", exception);
        }
    }

    private static SaveWorldGroupEditPayload decode(RegistryFriendlyByteBuf buffer) {
        byte[] compressed = buffer.readByteArray(MAX_COMPRESSED_BYTES);
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(compressed)))) {
            boolean manual = input.readBoolean();
            long baseRevision = input.readLong();
            int groupCount = bounded(input.readInt(), 0, MAX_GROUPS, "group count");
            int totalMembers = 0;
            List<ManualChunkGroupLayout.Group> groups = new ArrayList<>(groupCount);
            for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
                String id = input.readUTF(); String name = input.readUTF(); String icon = input.readUTF();
                String site = input.readUTF(); String resources = input.readUTF(); boolean configured = input.readBoolean();
                int members = bounded(input.readInt(), 0, MAX_MEMBERS, "member count");
                totalMembers += members;
                if (totalMembers > MAX_MEMBERS) throw new IllegalArgumentException("too many group members");
                List<String> tileIds = new ArrayList<>(members);
                for (int memberIndex = 0; memberIndex < members; memberIndex++) tileIds.add(input.readUTF());
                groups.add(new ManualChunkGroupLayout.Group(id, name, icon, site, resources, configured, tileIds));
            }
            return new SaveWorldGroupEditPayload(manual, baseRevision, groups);
        } catch (IOException exception) {
            throw new IllegalArgumentException("could not decode world group edit", exception);
        }
    }

    private static int bounded(int value, int minimum, int maximum, String label) {
        if (value < minimum || value > maximum) throw new IllegalArgumentException(label + " outside permitted range");
        return value;
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
