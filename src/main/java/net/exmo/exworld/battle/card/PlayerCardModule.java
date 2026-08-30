package net.exmo.exworld.battle.card;

import net.exmo.exworld.battle.skill.SkillRegistry;
import net.minecraft.server.MinecraftServer;
import net.exmo.exworld.progress.PlayerProgressSystem;
import net.exmo.exworld.progress.PlayerResourceVault;

import java.util.*;

/** Small interface over ownership, deck validation, starter content and persistence. */
public final class PlayerCardModule {
    private static final String DATA_NAME = "exworld_player_cards";
    private final SkillRegistry skills;

    public PlayerCardModule(SkillRegistry skills) { this.skills = skills; }

    public PlayerCardCollection collection(MinecraftServer server, UUID playerId) {
        return saved(server).get(playerId);
    }

    public List<String> battleDeck(MinecraftServer server, UUID playerId) {
        PlayerCardCollection collection = collection(server, playerId);
        if (!collection.deckPlayable()) throw new IllegalStateException("Battle deck must contain "
                + PlayerCardCollection.MIN_DECK_SIZE + "-" + PlayerCardCollection.MAX_DECK_SIZE + " owned cards");
        return collection.deck();
    }
    public List<Integer> battleDeckStars(MinecraftServer server, UUID playerId) {
        PlayerCardCollection collection = collection(server, playerId);
        if (!collection.deckPlayable()) throw new IllegalStateException("Battle deck is not playable");
        return collection.battleDeckInstances().stream().map(OwnedCardInstance::star).toList();
    }

    public int grant(MinecraftServer server, UUID playerId, String id, int amount) {
        requireSkill(id); int count = collection(server, playerId).grant(id, amount); saved(server).changed(); return count;
    }

    public boolean addToDeck(MinecraftServer server, UUID playerId, String id) {
        requireSkill(id); boolean changed = collection(server, playerId).addToDeck(id); if (changed) saved(server).changed(); return changed;
    }

    public boolean removeFromDeck(MinecraftServer server, UUID playerId, String id) {
        boolean changed = collection(server, playerId).removeFromDeck(id); if (changed) saved(server).changed(); return changed;
    }

    public void clearDeck(MinecraftServer server, UUID playerId) { collection(server, playerId).clearDeck(); saved(server).changed(); }

    /** Ensures a deterministic, deliberately small starter deck without leaking the full debug catalog. */
    public PlayerCardCollection ensureStarterDeck(MinecraftServer server, UUID playerId) {
        PlayerCardCollection collection = collection(server, playerId);
        if (ensureStarterDeck(collection)) saved(server).changed();
        return collection;
    }

    /** Pure collection seam used by tests and non-persistent callers. */
    public boolean ensureStarterDeck(PlayerCardCollection collection) {
        boolean removedLegacyDebug = skills.debugCardIds().isEmpty() && collection.owned().keySet().stream()
                .anyMatch(id -> id.startsWith("exworld:debug_card_"));
        if (removedLegacyDebug) collection.purgeCardsMatching(id -> id.startsWith("exworld:debug_card_"));
        if (collection.deckPlayable() && !removedLegacyDebug) return false;
        List<String> starter = skills.debugCardIds().isEmpty()
                ? skills.standardCardIds().stream().filter(id -> skills.card(id).map(CardDefinition::playable).orElse(false)).limit(8).toList()
                : skills.debugCardIds().stream().limit(8).toList();
        if (starter.size() < PlayerCardCollection.MIN_DECK_SIZE) {
            throw new IllegalStateException("Debug catalog does not contain enough starter cards");
        }
        boolean changed = removedLegacyDebug;
        for (String id : starter) {
            if (collection.count(id) == 0) {
                collection.grant(id, 1);
                changed = true;
            }
        }
        if (!collection.deck().equals(starter)) {
            if (!collection.replaceDeck(starter)) throw new IllegalStateException("Could not equip starter deck");
            changed = true;
        }
        return changed;
    }

    /** Explicit debug-only operation; never called by battle startup or the debug toggle. */
    public PlayerCardCollection grantAllDebugCards(MinecraftServer server, UUID playerId) {
        PlayerCardCollection collection = collection(server, playerId);
        boolean changed = false;
        for (String id : skills.debugCardIds()) {
            if (collection.count(id) == 0) {
                collection.grant(id, 1);
                changed = true;
            }
        }
        if (changed) saved(server).changed();
        return collection;
    }

    /** Administrative content grant: owns every registered standard card, plus opt-in debug cards. */
    public PlayerCardCollection grantAllCards(MinecraftServer server, UUID playerId) {
        PlayerCardCollection collection = collection(server, playerId);
        boolean changed = false;
        for (String id : skills.allCardIds()) {
            if (collection.count(id) == 0) {
                collection.grant(id, 1);
                changed = true;
            }
        }
        if (changed) saved(server).changed();
        return collection;
    }

    /** Explicit repair for worlds affected by the old all-card debug initializer. */
    public PlayerCardCollection resetDebugCardsToStarter(MinecraftServer server, UUID playerId) {
        PlayerCardCollection collection = collection(server, playerId);
        collection.purgeCardsMatching(id -> id.startsWith("exworld:debug_card_") || skills.isDebug(id));
        List<String> starter = skills.debugCardIds().isEmpty()
                ? skills.standardCardIds().stream().filter(id -> skills.card(id).map(CardDefinition::playable).orElse(false)).limit(8).toList()
                : skills.debugCardIds().stream().limit(8).toList();
        starter.forEach(id -> collection.grant(id, 1));
        if (!collection.replaceDeck(starter)) throw new IllegalStateException("Could not reset starter deck");
        saved(server).changed();
        return collection;
    }

    private void requireSkill(String id) {
        if (!skills.knownCard(id)) throw new IllegalArgumentException("Unknown card: " + id);
    }

    private PlayerCardSavedData saved(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerCardSavedData.FACTORY, DATA_NAME);
    }
    public void markChanged(MinecraftServer server) { saved(server).changed(); }

    /** Applies a result exactly once even if a returning session is replayed after a restart. */
    public boolean awardBattleResult(MinecraftServer server, String battleKey, UUID playerId, String cardId, int gold) {
        if (cardId != null && !cardId.isBlank()) requireSkill(cardId);
        boolean awarded = saved(server).awardOnce(battleKey + ":" + playerId, playerId, cardId, 0);
        if (awarded) PlayerProgressSystem.vault().add(server, playerId, PlayerResourceVault.GOLD, Math.max(0, gold));
        return awarded;
    }
    public boolean awardBattleResult(MinecraftServer server, String battleKey, UUID playerId, String cardId, int gold, Collection<String> additionalCards) {
        if (cardId != null && !cardId.isBlank()) requireSkill(cardId);
        additionalCards.forEach(this::requireSkill);
        boolean awarded = saved(server).awardOnce(battleKey + ":" + playerId, playerId, cardId, 0, additionalCards);
        if (awarded) PlayerProgressSystem.vault().add(server, playerId, PlayerResourceVault.GOLD, Math.max(0, gold));
        return awarded;
    }
    public boolean awardDungeonGold(MinecraftServer server, String runId, String rewardId, UUID playerId, int gold) {
        boolean awarded = saved(server).awardOnce("dungeon:" + runId + ":" + rewardId + ":" + playerId, playerId, null, 0);
        if (awarded) PlayerProgressSystem.vault().add(server, playerId, PlayerResourceVault.GOLD, Math.max(0, gold));
        return awarded;
    }
    /** Migrates the retired card-only gold field once by clearing it after transfer. */
    public void migrateGold(MinecraftServer server, UUID playerId) {
        int legacy = collection(server, playerId).drainGold();
        if (legacy > 0) { PlayerProgressSystem.vault().awardOnce(server, playerId, "migration:player_cards_gold", PlayerResourceVault.GOLD, legacy); saved(server).changed(); }
    }
}
