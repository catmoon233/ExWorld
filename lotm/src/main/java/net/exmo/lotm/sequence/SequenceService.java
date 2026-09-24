package net.exmo.lotm.sequence;

import net.exmo.exworld.Exworld;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SequenceService {
    private SequenceService() {}

    public static PlayerSequenceData data(Player player) {
        return player.getData(SequenceAttachments.SEQUENCE.get());
    }

    public static Optional<PathwayDefinition> pathway(Player player) {
        PlayerSequenceData data = data(player);
        if (!data.hasSequence()) return Optional.empty();
        return SequenceRegistry.findPathway(data.pathway());
    }

    public static Optional<SequenceDefinition> current(Player player) {
        PlayerSequenceData data = data(player);
        if (!data.hasSequence()) return Optional.empty();
        SequenceRank rank = SequenceRank.parse(data.rank()).orElse(null);
        return pathway(player).map(pathway -> pathway.byRank(rank));
    }

    public static List<SequenceDefinition> unlocked(Player player) {
        SequenceRank rank = SequenceRank.parse(data(player).rank()).orElse(null);
        return pathway(player).map(pathway -> pathway.unlockedThrough(rank)).orElse(List.of());
    }

    public static List<SequenceSkill> unlockedSkills(Player player) {
        List<SequenceSkill> skills = new ArrayList<>();
        for (SequenceDefinition sequence : unlocked(player)) skills.addAll(sequence.skills());
        return skills;
    }

    public static boolean ownsSpell(Player player, ResourceLocation spellId) {
        if (player == null || spellId == null) return false;
        for (SequenceSkill skill : unlockedSkills(player)) {
            if (skill.kind() == SkillKind.ACTIVE && spellId.equals(skill.ref())) return true;
        }
        return false;
    }

    public static List<SequenceSkill> ownedSpells(Player player) {
        List<SequenceSkill> spells = new ArrayList<>();
        for (SequenceSkill skill : unlockedSkills(player)) {
            if (skill.kind() == SkillKind.ACTIVE) spells.add(skill);
        }
        return spells;
    }

    public static boolean set(ServerPlayer player, PathwayDefinition pathway, SequenceRank rank) {
        if (player == null || pathway == null || rank == null || pathway.byRank(rank) == null) return false;
        data(player).set(pathway.id(), rank);
        reapply(player);
        SequenceNetwork.sync(player, false);
        return true;
    }

    public static void clear(ServerPlayer player) {
        if (player == null) return;
        data(player).clear();
        reapply(player);
        SequenceNetwork.sync(player, false);
    }

    public static void reapply(ServerPlayer player) {
        if (player == null) return;
        clearModifiers(player);
        float before = player.getMaxHealth();
        for (SequenceDefinition sequence : unlocked(player)) {
            for (AttributeGrant grant : sequence.attributes()) {
                apply(player, sequence.id(), grant);
            }
        }
        float gained = player.getMaxHealth() - before;
        if (gained > 0.0F) player.heal(gained);
    }

    private static void apply(ServerPlayer player, ResourceLocation sequenceId, AttributeGrant grant) {
        Holder<Attribute> holder = attribute(grant.attribute());
        if (holder == null) return;
        AttributeInstance instance = player.getAttribute(holder);
        if (instance == null) return;
        ResourceLocation id = modifierId(sequenceId, grant.attribute());
        instance.removeModifier(id);
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, grant.amount(), grant.operation()));
    }

    private static void clearModifiers(ServerPlayer player) {
        BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) return;
            List<ResourceLocation> remove = new ArrayList<>();
            for (AttributeModifier modifier : instance.getModifiers()) {
                ResourceLocation id = modifier.id();
                if (Exworld.MODID.equals(id.getNamespace()) && id.getPath().startsWith("sequence/")) {
                    remove.add(id);
                }
            }
            remove.forEach(instance::removeModifier);
        });
    }

    private static Holder<Attribute> attribute(ResourceLocation id) {
        if (id == null) return null;
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceKey.create(Registries.ATTRIBUTE, id)).orElse(null);
    }

    private static ResourceLocation modifierId(ResourceLocation sequenceId, ResourceLocation attribute) {
        String path = "sequence/" + sequenceId.getNamespace() + "." + sequenceId.getPath().replace('/', '.')
                + "/" + attribute.getNamespace() + "." + attribute.getPath().replace('/', '.');
        return ResourceLocation.fromNamespaceAndPath(Exworld.MODID, path);
    }
}
