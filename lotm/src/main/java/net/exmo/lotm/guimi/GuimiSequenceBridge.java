package net.exmo.lotm.guimi;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.sequences.Sequence;
import com.wan.gmmod.content.sequences.Sequences;
import com.wan.gmmod.content.witch.FemaleGenderCompat;
import com.wan.gmmod.content.witch.PromotionHooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.exmo.lotm.sequence.PlayerSequenceData;
import net.exmo.lotm.sequence.SequenceNetwork;
import net.exmo.lotm.sequence.SequenceRank;
import net.exmo.lotm.sequence.SequenceRegistry;
import net.exmo.lotm.sequence.SequenceService;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.sequence.SkillKind;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the ExWorld sheet and the guimi beyonder record on the same rung.
 * Does not replace pathway definitions upstream already registered.
 */
public final class GuimiSequenceBridge {
    private static final ThreadLocal<Boolean> PUSHING = ThreadLocal.withInitial(() -> false);
    private static final Set<UUID> WATCHING = ConcurrentHashMap.newKeySet();

    private GuimiSequenceBridge() {}

    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(GuimiSequenceBridge.class);
    }

    public static void reconcile(ServerPlayer player) {
        if (player == null) return;
        PlayerSequenceData data = SequenceService.data(player);
        if (!data.hasSequence()) {
            if (employed(player)) pullIfDiverged(player);
        } else if (GuimiPathways.guimiKey(data.pathway()).isEmpty()) {
            push(player);
        } else if (employed(player)) {
            pullIfDiverged(player);
        } else {
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
            if (key.isEmpty() || rank == null) {
                clearGuimi(player);
                return;
            }
            writeGuimi(player, key.get(), rank.number() < 0 ? 0 : rank.number());
        } finally {
            PUSHING.set(false);
        }
    }

    public static boolean cast(ServerPlayer player, String skillId) {
        if (player == null || skillId == null || skillId.isBlank()) return false;
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        if (id == null) return false;
        SequenceSkill owned = null;
        for (SequenceSkill skill : SequenceService.unlockedSkills(player)) {
            if (skill.kind() == SkillKind.ACTIVE && id.equals(skill.ref())) {
                owned = skill;
                break;
            }
        }
        if (owned == null) return false;
        AbstractSpell spell = SpellRegistry.getSpell(id);
        if (spell != null && spell != SpellRegistry.none()) {
            return spell.attemptInitiateCast(ItemStack.EMPTY, owned.level(), player.level(), player,
                    CastSource.SPELLBOOK, true, "sequence");
        }
        if ("guimi_mod".equals(id.getNamespace())) {
            push(player);
            Ability ability = AbilityRegistry.getById(id);
            if (ability == null || !ability.isActive()) return false;
            SkillManager.triggerAbility(player, ability);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!WATCHING.contains(player.getUUID())) return;
        if (player.level().getGameTime() % 20L != 0L) return;
        if (employed(player)) {
            if (pullIfDiverged(player)) SequenceNetwork.sync(player, false);
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
        if (SequenceRegistry.findPathway(id).isEmpty()) return false;
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
        if (SpiritualityManager.get(player) == 0) {
            SpiritualityManager.set(player, ModAttachments.DEFAULT_SPIRITUALITY);
        }
        if (sequence != null) PromotionHooks.onPromoted(player, sequence);
    }

    private static void clearGuimi(ServerPlayer player) {
        if (!employed(player) && player.getData(ModAttachments.SEQUENCE_LEVEL) == 0
                && SpiritualityManager.get(player) == 0
                && !Boolean.TRUE.equals(player.getData(ModAttachments.FEMALE_FORM))) {
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
        SpiritualityManager.set(player, 0);
    }

    private static boolean employed(ServerPlayer player) {
        return Sequences.employed(player.getData(ModAttachments.PATHWAY));
    }
}
