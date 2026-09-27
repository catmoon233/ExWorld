package net.exmo.exkeys.client;

import net.exmo.exkeys.KeyPolicy;
import net.exmo.exkeys.PolicyMode;
import net.exmo.exkeys.PolicySearch;
import net.exmo.exkeys.mixin.OptionsSubScreenAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class KeyPolicyScreen extends Screen {
    private final Screen parent;
    private final Screen grandparent;
    private EditBox search;
    private PolicyList list;
    private Button save;
    private Button filter;
    private boolean configuredOnly;
    private KeyPolicy draft = KeyPolicy.EMPTY;
    private String baseline = KeyPolicy.EMPTY.toJson();
    private boolean dirty;
    private String status = "";

    public KeyPolicyScreen(Screen parent) {
        super(Component.translatable("exkeys.screen.title"));
        this.parent = parent;
        this.grandparent = parent instanceof KeyBindsScreen binds
                ? ((OptionsSubScreenAccess) binds).exkeys$lastScreen()
                : parent;
    }

    @Override
    protected void init() {
        if (!dirty) {
            draft = ExKeysClient.active();
            baseline = draft.toJson();
        }
        int panel = Math.min(760, width - 24);
        int left = (width - panel) / 2;
        search = new EditBox(font, left, 48, panel - 100, 18, Component.translatable("exkeys.search"));
        search.setHint(Component.translatable("exkeys.search.hint"));
        search.setMaxLength(64);
        search.setResponder(text -> rebuild(false));
        addRenderableWidget(search);
        filter = Button.builder(filterLabel(), button -> {
            configuredOnly = !configuredOnly;
            button.setMessage(filterLabel());
            rebuild(false);
        }).bounds(left + panel - 92, 48, 92, 18).build();
        addRenderableWidget(filter);
        int listTop = 72;
        list = new PolicyList(this, minecraft, left, listTop, panel, height - listTop - 36);
        addRenderableWidget(list);
        save = Button.builder(saveLabel(), button -> save()).bounds(width / 2 - 154, height - 28, 148, 20).build();
        addRenderableWidget(save);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 + 4, height - 28, 80, 20).build());
        setInitialFocus(search);
        refreshStatus();
        rebuild(true);
        if (ExKeysClient.mode() == PolicyMode.Kind.REMOTE) ExKeysClient.pull();
    }

    boolean canEdit() {
        return ExKeysClient.canEdit();
    }

    void setRule(String id, boolean hide, boolean block, boolean opTrigger, boolean opDisplay) {
        if (!canEdit()) return;
        draft = draft.with(id, hide, block, opTrigger, opDisplay);
        markDirty();
    }

    private void markDirty() {
        dirty = !draft.toJson().equals(baseline);
        refreshStatus();
        rebuild(false);
    }

    public void onRemote(KeyPolicy policy, boolean editable) {
        if (policy == null) return;
        if (policy.toJson().equals(draft.toJson())) {
            draft = policy;
            baseline = policy.toJson();
            dirty = false;
        } else if (!dirty) {
            draft = policy;
            baseline = policy.toJson();
            rebuild(false);
        }
        refreshStatus();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int panel = Math.min(760, width - 24);
        int left = (width - panel) / 2;
        graphics.drawString(font, title, left, 12, 0xFFF3E6C4, false);
        graphics.drawString(font, status, left, 26, 0xFFB7AA96, false);
        String hint = Component.translatable("exkeys.screen.hint").getString();
        graphics.drawString(font, font.plainSubstrByWidth(hint, panel), left, 38, 0xFF8E8476, false);
    }

    @Override
    public void onClose() {
        if (minecraft == null) return;
        if (parent instanceof KeyBindsScreen) {
            minecraft.setScreen(new KeyBindsScreen(grandparent, minecraft.options));
        } else {
            minecraft.setScreen(parent);
        }
    }

    private void save() {
        if (!canEdit() || !dirty) return;
        if (ExKeysClient.mode() == PolicyMode.Kind.LOCAL) {
            if (ExKeysClient.saveLocal(draft)) {
                baseline = draft.toJson();
                dirty = false;
                status = Component.translatable("exkeys.status.saved").getString();
            } else {
                status = Component.translatable("exkeys.status.write_failed").getString();
            }
            updateSave();
            return;
        }
        ExKeysClient.push(draft);
        status = Component.translatable("exkeys.status.pending").getString();
        updateSave();
    }

    private void rebuild(boolean resetScroll) {
        if (list == null || minecraft == null || minecraft.options == null) return;
        double scroll = resetScroll ? 0 : list.getScrollAmount();
        List<PolicyList.RowModel> rows = new ArrayList<>();
        KeyMapping[] mappings = minecraft.options.keyMappings.clone();
        Arrays.sort(mappings);
        String query = search == null ? "" : search.getValue();
        String lastCategory = null;
        List<PolicyList.RowModel> pending = new ArrayList<>();
        for (KeyMapping mapping : mappings) {
            String category = Component.translatable(mapping.getCategory()).getString();
            String name = Component.translatable(mapping.getName()).getString();
            String bound = mapping.getTranslatedKeyMessage().getString();
            KeyPolicy.Rule rule = draft.rule(mapping.getName());
            if (configuredOnly && !rule.active()) continue;
            if (!PolicySearch.matches(query, mapping.getName(), name, category, bound)) continue;
            if (!category.equals(lastCategory)) {
                rows.addAll(pending);
                pending.clear();
                pending.add(new PolicyList.RowModel.Category(category));
                lastCategory = category;
            }
            pending.add(new PolicyList.RowModel.Binding(mapping.getName(), name, bound, rule.hide(), rule.block(), rule.opTrigger(), rule.opDisplay()));
        }
        if (pending.size() > 1) rows.addAll(pending);
        list.setRows(rows, scroll);
        updateSave();
    }

    private void refreshStatus() {
        if (ExKeysClient.mode() == PolicyMode.Kind.LOCAL) {
            status = Component.translatable(dirty ? "exkeys.status.local_dirty" : "exkeys.status.local").getString();
        } else if (!canEdit()) {
            status = Component.translatable("exkeys.status.readonly").getString();
        } else if (dirty) {
            status = Component.translatable("exkeys.status.remote_dirty").getString();
        } else {
            status = Component.translatable("exkeys.status.remote").getString();
        }
        updateSave();
    }

    private void updateSave() {
        if (save == null) return;
        save.setMessage(saveLabel());
        save.active = canEdit() && dirty;
    }


    private Component saveLabel() {
        if (!canEdit()) return Component.translatable("exkeys.save.readonly");
        return Component.translatable(ExKeysClient.mode() == PolicyMode.Kind.LOCAL ? "exkeys.save.local" : "exkeys.save.remote");
    }

    private Component filterLabel() {
        return Component.translatable(configuredOnly ? "exkeys.filter.configured" : "exkeys.filter.all");
    }
}
