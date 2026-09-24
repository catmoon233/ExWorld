package net.exmo.lotm.sequence;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.sequences.Sequence;
import com.wan.gmmod.content.sequences.Sequences;
import com.wan.gmmod.content.witch.FemaleGenderCompat;
import com.wan.gmmod.content.witch.PromotionHooks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Imports guimi pathways into the ExWorld sheet and keeps the two player records on the same rung.
 * Skills on each rung are the guimi abilities registered for that sequence.
 */
public final class GuimiSequenceBridge {
    private static final ThreadLocal<Boolean> PUSHING = ThreadLocal.withInitial(() -> false);
    private static final Set<UUID> WATCHING = ConcurrentHashMap.newKeySet();

    private GuimiSequenceBridge() {}

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(GuimiSequenceBridge.class);
    }

    @SubscribeEvent
    public static void registerPathways(RegisterSequencesEvent event) {
        com.wan.gmmod.content.sequences.SequenceRegistry.init();
        for (Sequences.Pathway pathway : Sequences.Pathway.values()) {
            String canonical = GuimiPathways.canonicalPath(pathway.getKey());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GuimiPathways.NAMESPACE, canonical);
            PathwayDefinition.Builder builder = PathwayDefinition.builder(id, pathway.getDisplayName());
            for (int level = Sequences.MAX_LEVEL; level >= 0; level--) {
                Sequence sequence = com.wan.gmmod.content.sequences.SequenceRegistry.get(pathway, level);
                if (sequence == null) continue;
                SequenceRank rank = SequenceRank.parse(Integer.toString(level)).orElseThrow();
                builder.sequence(rank, sequence.getName(), sequence.getDescription(), List.of(), skills(sequence));
            }
            builder.sequence(SequenceRank.OLD_ONE, "旧日",
                    pathway.getDisplayName() + "途径 · 旧日。诡秘侧按序列零同步。", List.of(), List.of());
            builder.sequence(SequenceRank.PILLAR, "支柱",
                    pathway.getDisplayName() + "途径 · 支柱。诡秘侧按序列零同步。", List.of(), List.of());
            event.register(builder.build());
            alias(pathway.getKey(), id);
            alias("lotm:" + pathway.getKey(), id);
            alias("guimi_mod:" + pathway.getKey(), id);
        }
        for (Map.Entry<String, String> entry : GuimiPathways.aliases().entrySet()) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GuimiPathways.NAMESPACE, entry.getValue());
            alias(entry.getKey(), id);
            alias("lotm:" + entry.getKey(), id);
        }
    }

    public static void reconcile(ServerPlayer player) {
        if (player == null) return;
        if (employed(player)) {
            pullIfDiverged(player);
        } else if (GuimiPathways.guimiKey(SequenceService.data(player).pathway()).isPresent()) {
            push(player);
        }
        WATCHING.add(player.getUUID());
    }

    public static void push(ServerPlayer player) {
        if (player == null || PUSHING.get()) return;
        PUSHING.set(true);
        try {
            PlayerSequenceData data = SequenceService.data(player);
            if (!data.hasSequence()) {
                clearGuimi(player);
                return;
            }
            Optional<String> key = GuimiPathways.guimiKey(data.pathway());
            SequenceRank rank = SequenceRank.parse(data.rank()).orElse(null);
            if (key.isEmpty() || rank == null) return;
            writeGuimi(player, key.get(), rank.number() < 0 ? 0 : rank.number());
        } finally {
            PUSHING.set(false);
        }
    }

    public static boolean cast(ServerPlayer player, String skillId) {
        if (player == null || skillId == null || skillId.isBlank()) return false;
        push(player);
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        if (id == null) return false;
        boolean owned = false;
        for (SequenceSkill skill : SequenceService.unlockedSkills(player)) {
            if (skill.kind() == SkillKind.ACTIVE && id.equals(skill.ref())) {
                owned = true;
                break;
            }
        }
        if (!owned) return false;
        Ability ability = AbilityRegistry.getById(id);
        if (ability == null || !ability.isActive()) return false;
        SkillManager.triggerAbility(player, ability);
        return true;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!WATCHING.contains(player.getUUID())) return;
        if (player.level().getGameTime() % 20L != 0L) return;
        if (employed(player)) {
            if (pullIfDiverged(player)) {
                SequenceNetwork.sync(player, false);
            }
            return;
        }
        if (GuimiPathways.guimiKey(SequenceService.data(player).pathway()).isPresent()) {
            writeExworld(player, null, null);
            SequenceNetwork.sync(player, false);
        }
    }

    private static boolean pullIfDiverged(ServerPlayer player) {
        String key = player.getData(ModAttachments.PATHWAY);
        int level = player.getData(ModAttachments.SEQUENCE_LEVEL);
        if (!Sequences.employed(key)) return false;
        PlayerSequenceData data = SequenceService.data(player);
        if (projected(data, key, level)) return false;
        SequenceRank rank = SequenceRank.parse(Integer.toString(level)).orElse(null);
        if (rank == null) return false;
        String canonical = GuimiPathways.canonicalPath(key);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GuimiPathways.NAMESPACE, canonical);
        if (matches(data, id, rank, key)) return false;
        writeExworld(player, id, rank);
        return true;
    }

    private static boolean matches(PlayerSequenceData data, ResourceLocation id, SequenceRank rank, String guimiKey) {
        if (!data.hasSequence() || !data.rank().equals(rank.token())) return false;
        if (data.pathway().equals(id.toString())) return true;
        return GuimiPathways.guimiKey(data.pathway()).orElse("").equals(guimiKey);
    }

    private static boolean projected(PlayerSequenceData data, String guimiKey, int level) {
        if (level != 0 || !data.hasSequence()) return false;
        SequenceRank rank = SequenceRank.parse(data.rank()).orElse(null);
        if (rank != SequenceRank.OLD_ONE && rank != SequenceRank.PILLAR) return false;
        return GuimiPathways.guimiKey(data.pathway()).orElse("").equals(guimiKey);
    }

    private static void writeExworld(ServerPlayer player, ResourceLocation pathwayId, SequenceRank rank) {
        PUSHING.set(true);
        try {
            if (pathwayId == null || rank == null) {
                SequenceService.data(player).clear();
            } else {
                SequenceService.data(player).set(pathwayId, rank);
            }
            SequenceService.reapply(player);
        } finally {
            PUSHING.set(false);
        }
    }

    private static void writeGuimi(ServerPlayer player, String key, int level) {
        Sequences.Pathway pathway = Sequences.fromKey(key);
        if (pathway == null) return;
        String currentKey = player.getData(ModAttachments.PATHWAY);
        int currentLevel = player.getData(ModAttachments.SEQUENCE_LEVEL);
        if (key.equals(currentKey) && level == currentLevel && Sequences.employed(currentKey)) return;
        Sequence sequence = com.wan.gmmod.content.sequences.SequenceRegistry.get(pathway, level);
        player.setData(ModAttachments.SEQUENCE_LEVEL, level);
        player.setData(ModAttachments.PATHWAY, key);
        if (sequence != null) {
            player.setData(ModAttachments.ACTING_SEQUENCE_ID, sequence.getId().toString());
            player.setData(ModAttachments.ACTING_PROGRESS, 0);
        }
        if (player.getData(ModAttachments.SPIRITUALITY) == 0) {
            player.setData(ModAttachments.SPIRITUALITY, ModAttachments.DEFAULT_SPIRITUALITY);
        }
        if (sequence != null) PromotionHooks.onPromoted(player, sequence);
    }

    private static void clearGuimi(ServerPlayer player) {
        if (!employed(player) && player.getData(ModAttachments.SEQUENCE_LEVEL) == 0
                && player.getData(ModAttachments.SPIRITUALITY) == 0) {
            return;
        }
        if (Boolean.TRUE.equals(player.getData(ModAttachments.FEMALE_FORM))) {
            player.setData(ModAttachments.FEMALE_FORM, false);
            FemaleGenderCompat.setFemale(player, false);
        }
        player.setData(ModAttachments.SEQUENCE_LEVEL, 0);
        player.setData(ModAttachments.PATHWAY, "");
        player.setData(ModAttachments.ACTING_SEQUENCE_ID, "");
        player.setData(ModAttachments.ACTING_PROGRESS, 0);
        player.setData(ModAttachments.SPIRITUALITY, 0);
    }

    private static boolean employed(ServerPlayer player) {
        return Sequences.employed(player.getData(ModAttachments.PATHWAY));
    }

    private static List<SequenceSkill> skills(Sequence sequence) {
        List<SequenceSkill> skills = new ArrayList<>();
        for (Ability ability : AbilityRegistry.getAbilitiesFor(sequence.getId())) {
            String icon = "tex:" + ability.getIconTexture();
            if (ability.isActive()) {
                skills.add(new SequenceSkill(SkillKind.ACTIVE, ability.getId(), 1,
                        ability.getNameKey(), ability.getNameKey(), icon));
            } else {
                skills.add(SequenceSkill.passive(ability.getId(), ability.getNameKey(), ability.getNameKey(), icon));
            }
        }
        return skills;
    }

    private static void alias(String token, ResourceLocation id) {
        SequenceRegistry.alias(token, id);
    }
}
