package net.exmo.exworld.battle.card;

import java.util.*;

/** Mutable deck aggregate whose shuffle is deterministic for a BattleSession seed. */
public final class DeckState {
    public static final int DEFAULT_INITIAL_HAND = 3;
    public static final int DRAW_PER_PHASE = 2;
    public static final int HAND_LIMIT = 7;
    private static final int MAX_DRAW_OPERATIONS = 64;
    private final ArrayDeque<SkillCard> drawPile = new ArrayDeque<>();
    private final List<SkillCard> hand = new ArrayList<>();
    private final List<SkillCard> discard = new ArrayList<>();
    private final List<SkillCard> exhausted = new ArrayList<>();
    private final List<SkillCard> innate = new ArrayList<>();
    private final Random random;

    public DeckState(List<String> skills, long seed) {
        this(skills, java.util.Collections.nCopies(skills.size(), 1), seed);
    }
    public DeckState(List<String> skills, List<Integer> stars, long seed) {
        random = new Random(seed);
        List<SkillCard> cards = new ArrayList<>();
        for (int i = 0; i < skills.size(); i++) cards.add(new SkillCard(UUID.randomUUID(), skills.get(i), stars.get(i), false, false));
        // Each battle owns a seeded random sequence. Shuffle both its initial draw pile and every discard recycle.
        Collections.shuffle(cards, random);
        drawPile.addAll(cards);
        innate.add(SkillCard.innate("exworld:basic_attack"));
    }

    public DeckState(List<SkillCard> draw, List<SkillCard> hand, List<SkillCard> discard,
                     List<SkillCard> exhausted, List<SkillCard> innate, long seed) {
        random = new Random(seed); drawPile.addAll(draw); this.hand.addAll(hand); this.discard.addAll(discard);
        this.exhausted.addAll(exhausted); this.innate.addAll(innate);
    }

    public void drawInitial() { drawInitial(DEFAULT_INITIAL_HAND); }
    public void drawInitial(int amount) { draw(Math.max(0, Math.min(HAND_LIMIT, amount))); }
    public void drawForPhase() { drawForPhase(DRAW_PER_PHASE); }
    public void drawForPhase(int amount) { draw(Math.max(0, amount)); }
    public void endPhase(java.util.function.Predicate<SkillCard> retained) {
        endPhase(retained, ignored -> false);
    }
    public void endPhase(java.util.function.Predicate<SkillCard> retained, java.util.function.Predicate<SkillCard> ethereal) {
        List<SkillCard> released = hand.stream().filter(card -> !retained.test(card) || ethereal.test(card)).toList();
        hand.removeAll(released);
        released.forEach(card -> { if (ethereal.test(card) || card.exhaust()) exhausted.add(card); else discard.add(card); });
    }
    public Optional<SkillCard> card(UUID id) {
        return java.util.stream.Stream.concat(hand.stream(), innate.stream()).filter(card -> card.instanceId().equals(id)).findFirst();
    }
    public boolean consume(UUID id) {
        return consume(id, false);
    }
    public boolean consume(UUID id, boolean exhaust) {
        Optional<SkillCard> selected = hand.stream().filter(card -> card.instanceId().equals(id)).findFirst();
        if (selected.isEmpty()) return innate.stream().anyMatch(card -> card.instanceId().equals(id));
        SkillCard card = selected.get(); hand.remove(card); (card.exhaust() || exhaust ? exhausted : discard).add(card); return true;
    }
    public int drawCards(int count) { int before = hand.size(); draw(count); return hand.size() - before; }
    public void addToDiscard(String skillId) { if (skillId != null && !skillId.isBlank()) discard.add(SkillCard.drawn(skillId)); }
    public void addToDrawPile(String skillId) { if (skillId != null && !skillId.isBlank()) drawPile.addLast(SkillCard.drawn(skillId)); }
    public boolean addToHandOrDraw(String skillId) {
        if (skillId == null || skillId.isBlank()) return false;
        if (hand.size() < HAND_LIMIT) hand.add(SkillCard.drawn(skillId));
        else drawPile.addFirst(SkillCard.drawn(skillId));
        return true;
    }
    /** Removes one card from the battle deck. The current card is still in hand and is therefore never selected. */
    public Optional<SkillCard> purgeOne() {
        if (!drawPile.isEmpty()) return Optional.of(drawPile.removeFirst());
        if (!discard.isEmpty()) return Optional.of(discard.remove(0));
        return Optional.empty();
    }
    public List<SkillCard> handWithInnate() {
        return java.util.stream.Stream.concat(hand.stream(), innate.stream()).toList();
    }
    public int drawCount() { return drawPile.size(); }
    public int discardCount() { return discard.size(); }
    public List<SkillCard> drawPile() { return List.copyOf(drawPile); }
    public List<SkillCard> hand() { return List.copyOf(hand); }
    public List<SkillCard> discardPile() { return List.copyOf(discard); }
    public List<SkillCard> exhaustedPile() { return List.copyOf(exhausted); }
    public List<SkillCard> innateCards() { return List.copyOf(innate); }

    private void draw(int count) {
        int operations = 0;
        for (int i = 0; i < count && hand.size() < HAND_LIMIT && operations++ < MAX_DRAW_OPERATIONS; i++) {
            if (drawPile.isEmpty()) reshuffle();
            if (drawPile.isEmpty()) return;
            hand.add(drawPile.removeFirst());
        }
    }
    private void reshuffle() { Collections.shuffle(discard, random); drawPile.addAll(discard); discard.clear(); }
}
