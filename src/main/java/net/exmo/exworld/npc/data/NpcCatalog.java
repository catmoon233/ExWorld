package net.exmo.exworld.npc.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** World store for documents and the relation graph. Entities only keep a document id. */
public final class NpcCatalog extends SavedData {
    public static final String ID = "exworld_urban_npcs";
    public static final Factory<NpcCatalog> FACTORY = new Factory<>(NpcCatalog::new, NpcCatalog::load);

    private final Map<String, NpcDocument> documents = new LinkedHashMap<>();
    private final List<RelationEdge> relations = new ArrayList<>();
    private int revision = 1;

    public NpcCatalog() {
        NpcPresets.seed(this);
    }

    private NpcCatalog(boolean seed) {
        if (seed) NpcPresets.seed(this);
    }

    public static NpcCatalog get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public int revision() { return revision; }

    public List<String> ids() { return new ArrayList<>(documents.keySet()); }

    public Optional<NpcDocument> document(String id) { return Optional.ofNullable(documents.get(id)); }

    public List<NpcDocument> documents() { return new ArrayList<>(documents.values()); }

    public List<RelationEdge> relations() { return List.copyOf(relations); }

    public List<RelationEdge> outgoing(String fromId) {
        return relations.stream().filter(edge -> edge.fromId().equals(fromId)).toList();
    }

    public void put(NpcDocument document, List<RelationEdge> outgoing) {
        documents.put(document.id(), document);
        relations.removeIf(edge -> edge.fromId().equals(document.id()));
        if (outgoing != null) relations.addAll(outgoing);
        revision++;
        setDirty();
    }
    public boolean remove(String id) {
        if (id == null || !documents.containsKey(id)) return false;
        documents.remove(id);
        relations.removeIf(edge -> edge.fromId().equals(id) || edge.toId().equals(id));
        revision++;
        setDirty();
        return true;
    }


    public void addRelation(RelationEdge edge) {
        relations.add(edge);
        revision++;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag docs = new ListTag();
        for (NpcDocument document : documents.values()) docs.add(NpcCodec.saveDocument(document));
        tag.put("documents", docs);
        ListTag edges = new ListTag();
        for (RelationEdge edge : relations) edges.add(NpcCodec.saveEdge(edge));
        tag.put("relations", edges);
        tag.putInt("revision", revision);
        return tag;
    }

    private static NpcCatalog load(CompoundTag tag, HolderLookup.Provider registries) {
        NpcCatalog catalog = new NpcCatalog(false);
        ListTag docs = tag.getList("documents", Tag.TAG_COMPOUND);
        for (int i = 0; i < docs.size(); i++) {
            try {
                NpcDocument document = NpcCodec.loadDocument(docs.getCompound(i));
                if (!document.id().isBlank()) catalog.documents.put(document.id(), document);
            } catch (RuntimeException ignored) {
            }
        }
        ListTag edges = tag.getList("relations", Tag.TAG_COMPOUND);
        for (int i = 0; i < edges.size(); i++) {
            try {
                catalog.relations.add(NpcCodec.loadEdge(edges.getCompound(i)));
            } catch (RuntimeException ignored) {
            }
        }
        catalog.revision = Math.max(1, tag.getInt("revision"));
        if (catalog.documents.isEmpty()) NpcPresets.seed(catalog);
        return catalog;
    }
}
