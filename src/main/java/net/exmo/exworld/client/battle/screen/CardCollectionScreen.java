package net.exmo.exworld.client.battle.screen;

import net.exmo.exworld.battle.card.*;
import net.exmo.exworld.network.CardCollectionActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * Classic MMORPG collection layout: category rail, inventory grid, deck column and inspected-card pane.
 */
public final class CardCollectionScreen extends Screen {
    private static CardCollectionSnapshot latest;
    private int page, deckSlot, sortMode;
    private String query = "", rarity = "all", category = "all";
    private UUID selectedCard;
    private final Set<UUID> fusion = new LinkedHashSet<>();
    private EditBox search, rename;

    public CardCollectionScreen() {
        super(Component.translatable("screen.exworld.card_collection"));
    }

    public static void install(CardCollectionSnapshot value) {
        latest = value;
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof CardCollectionScreen screen)
            screen.rebuildWidgets();
        else net.minecraft.client.Minecraft.getInstance().setScreen(new CardCollectionScreen());
    }

    public void renderBackground2(GuiGraphics g, int mx, int my, float p) {
        g.fill(0, 0, width, height, 0xED080D14);
    }
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float p) {

    }

    @Override
    protected void init() {
        if (latest == null) {
            request(action(CardCollectionActionPayload.Action.REQUEST, null, List.of(), ""));
            return;
        }
        deckSlot = Math.max(0, Math.min(deckSlot, latest.decks().size() - 1));
        int left = 18, right = width - 238;
        search = new EditBox(font, left + 82, 36, Math.max(120, right - left - 280), 20, Component.translatable("screen.exworld.search"));
        search.setHint(Component.translatable("screen.exworld.search_hint"));
        search.setValue(query);
        search.setResponder(v -> {
            query = v;
            page = 0;
        });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.sort_" + sortMode), b -> {
            sortMode = (sortMode + 1) % 3;
            rebuildWidgets();
        }).bounds(right - 180, 36, 84, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.rarity_" + rarity), b -> {
            rarity = switch (rarity) {
                case "all" -> "common";
                case "common" -> "rare";
                default -> "all";
            };
            page = 0;
            rebuildWidgets();
        }).bounds(right - 90, 36, 84, 20).build());
        int deckX = width - 222;
        for (int i = 0; i < latest.decks().size(); i++) {
            int slot = i;
            var deck = latest.decks().get(i);
            addRenderableWidget(Button.builder(Component.literal((i == latest.activeDeck() ? "◆ " : "") + deck.name()), b -> {
                deckSlot = slot;
                page = 0;
                rebuildWidgets();
            }).bounds(deckX + 8, 40 + i * 25, 198, 21).build());
        }
        rename = new EditBox(font, deckX + 8, 172, 136, 20, Component.translatable("screen.exworld.rename"));
        rename.setMaxLength(32);
        rename.setValue(latest.decks().get(deckSlot).name());
        addRenderableWidget(rename);
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.rename_short"), b -> request(action(CardCollectionActionPayload.Action.RENAME, null, List.of(), rename.getValue()))).bounds(deckX + 148, 172, 58, 20).build());
        List<OwnedCardInstance> cards = filtered().stream().skip(page * 28L).limit(28).toList();
        int gridX = left + 82, gridY = 68, cellW = Math.max(78, Math.min(104, (right - gridX - 10) / 7));
        for (int i = 0; i < cards.size(); i++) {
            OwnedCardInstance card = cards.get(i);
            int x = gridX + (i % 7) * cellW, y = gridY + (i / 7) * 54;
            Button button = Button.builder(Component.literal(displayName(card.cardId()) + "  ★" + card.star()), b -> {
                selectedCard = card.id();
                if (Screen.hasShiftDown()) {
                    if (!fusion.add(card.id())) fusion.remove(card.id());
                }
                rebuildWidgets();
            }).bounds(x, y, cellW - 5, 48).build();
            addRenderableWidget(button);
        }
        addRenderableWidget(Button.builder(Component.literal("‹"), b -> {
            if (page > 0) {
                page--;
                rebuildWidgets();
            }
        }).bounds(gridX, height - 38, 28, 21).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal("›"), b -> {
            page++;
            rebuildWidgets();
        }).bounds(gridX + 34, height - 38, 28, 21).build()).active = (page + 1) * 28 < filtered().size();
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.fuse_cards"), b -> {
            request(action(CardCollectionActionPayload.Action.FUSE, null, List.copyOf(fusion), ""));
            fusion.clear();
        }).bounds(left, height - 38, 72, 21).build()).active = fusion.size() == 3;
        Button add = Button.builder(Component.translatable("screen.exworld.add_to_deck"), b -> {
            if (selectedCard != null)
                request(action(CardCollectionActionPayload.Action.ADD_TO_DECK, selectedCard, List.of(), ""));
        }).bounds(right - 126, height - 38, 120, 21).build();
        add.active = selectedCard != null && !latest.decks().get(deckSlot).cardIds().contains(selectedCard);
        addRenderableWidget(add);
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.set_active"), b -> request(action(CardCollectionActionPayload.Action.SET_ACTIVE, null, List.of(), ""))).bounds(deckX + 8, height - 38, 198, 21).build()).active = latest.activeDeck() != deckSlot;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float p) {
        renderBackground2(g, mx, my, p);
        int left = 18, right = width - 238, deckX = width - 222;
        panel(g, left, 14, right - left, height - 26, 0xFF58708B);
        panel(g, deckX, 14, 214, height - 26, 0xFFF2CB72);
        g.drawString(font, title, left + 14, 22, 0xFFF2CB72, true);
        g.drawString(font, Component.translatable("screen.exworld.collection_meta", latest == null ? 0 : latest.cards().size(), latest == null ? 0 : latest.gold()), deckX + 10, 22, 0xFFF2CB72, false);
        g.fill(left + 10, 34, left + 72, height - 48, 0xCC111923);
        String[] tabs = {"all", "attack", "skill", "power", "status", "curse"};
        for (int i = 0; i < tabs.length; i++)
            g.drawCenteredString(font, Component.translatable("screen.exworld.category_" + tabs[i]), left + 41, 72 + i * 34, category.equals(tabs[i]) ? 0xFFF2CB72 : 0xFF91A0AF);
        if (latest != null) {
            var deck = latest.decks().get(deckSlot);
            g.drawString(font, Component.translatable("screen.exworld.deck_count", deck.cardIds().size()), deckX + 10, 202, 0xFFF4F6F8);
            int y = 219;
            for (UUID id : deck.cardIds().stream().limit(Math.max(1, (height - 278) / 14)).toList()) {
                OwnedCardInstance card = card(id);
                if (card != null) {
                    g.drawString(font, displayName(card.cardId()) + "  ★" + card.star(), deckX + 12, y, 0xFFD8E0E8, false);
                    g.drawString(font, "×", deckX + 190, y, 0xFFFF7A82, false);
                    y += 14;
                }
            }
            OwnedCardInstance selected = card(selectedCard);
            if (selected != null) {
                var def = latest.definitions().stream().filter(v -> v.id().equals(selected.cardId())).findFirst().orElse(null);
                if (def != null) {
                    int dx = left + 84, dy = Math.min(height - 130, 294);
                    g.fill(dx, dy, right - 8, height - 48, 0xD917202B);
                    g.drawString(font, Component.translatable(def.nameKey()), dx + 10, dy + 9, 0xFFF2CB72, true);
                    g.drawWordWrap(font, Component.translatable(def.descriptionKey()), dx + 10, dy + 25, right - dx - 28, 0xFFCCD5DF);
                    var tier = def.stars().get(selected.star() - 1);
                    g.drawString(font, Component.translatable("screen.exworld.card_stats", tier.manaCost(), tier.range(), "★".repeat(selected.star())), dx + 10, dy + 56, 0xFF83B9FF, false);
                }
            }
            g.drawString(font, Component.translatable("screen.exworld.page", page + 1, Math.max(1, (filtered().size() + 27) / 28)), left + 154, height - 33, 0xFF91A0AF);
            g.drawString(font, Component.translatable("screen.exworld.fusion_selected", fusion.size()), left + 220, height - 33, 0xFF91A0AF);
        }
        super.render(g, mx, my, p);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (latest != null && button == 0 && x >= 18 && x < 90 && y >= 58 && y < 58 + 6 * 34) {
            int tab = (int) ((y - 58) / 34);
            String[] tabs = {"all", "attack", "skill", "power", "status", "curse"};
            if (tab >= 0 && tab < tabs.length) { category = tabs[tab]; page = 0; rebuildWidgets(); return true; }
        }
        if (latest != null && button == 0) {
            int deckX = width - 222, py = 219;
            for (UUID id : latest.decks().get(deckSlot).cardIds().stream().limit(Math.max(1, (height - 278) / 14)).toList()) {
                if (x >= deckX + 8 && x < deckX + 206 && y >= py - 2 && y < py + 11) {
                    request(action(CardCollectionActionPayload.Action.REMOVE_FROM_DECK, id, List.of(), ""));
                    return true;
                }
                py += 14;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    private List<OwnedCardInstance> filtered() {
        if (latest == null) return List.of();
        Comparator<OwnedCardInstance> c = switch (sortMode) {
            case 1 ->
                    Comparator.comparingInt(OwnedCardInstance::star).reversed().thenComparing(OwnedCardInstance::cardId);
            case 2 -> Comparator.comparing(OwnedCardInstance::cardId).reversed();
            default -> Comparator.comparing(OwnedCardInstance::cardId);
        };
        String n = query.toLowerCase(Locale.ROOT);
        return latest.cards().stream()
                .filter(v -> n.isBlank() || v.cardId().toLowerCase(Locale.ROOT).contains(n) || displayName(v.cardId()).toLowerCase(Locale.ROOT).contains(n))
                .filter(v -> rarity.equals("all") || latest.definitions().stream().anyMatch(d -> d.id().equals(v.cardId()) && d.rarity().equals(rarity)))
                .filter(v -> category.equals("all") || latest.definitions().stream().anyMatch(d -> d.id().equals(v.cardId()) && d.tags().contains("type:" + category)))
                .sorted(c).toList();
    }

    private OwnedCardInstance card(UUID id) {
        return latest == null ? null : latest.cards().stream().filter(v -> v.id().equals(id)).findFirst().orElse(null);
    }

    private CardCollectionActionPayload action(CardCollectionActionPayload.Action a, UUID card, List<UUID> materials, String value) {
        return new CardCollectionActionPayload(a, deckSlot, card, materials, value);
    }

    private static void request(CardCollectionActionPayload p) {
        PacketDistributor.sendToServer(p);
    }

    private static String displayName(String id) {
        if (latest != null) {
            var definition = latest.definitions().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
            if (definition != null) return shorten(Component.translatable(definition.nameKey()).getString(), 14);
        }
        String value = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return shorten(value, 14);
    }

    private static String shorten(String value, int limit) {
        return value.length() > limit ? value.substring(0, Math.max(1, limit - 1)) + "…" : value;
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h, int edge) {
        g.fill(x + 5, y, x + w - 5, y + h, 0xE910171F);
        g.fill(x, y + 5, x + w, y + h - 5, 0xE910171F);
        g.fill(x + 5, y, x + w - 5, y + 1, edge);
    }
}
