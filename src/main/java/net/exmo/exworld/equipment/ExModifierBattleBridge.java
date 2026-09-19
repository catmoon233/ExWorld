package net.exmo.exworld.equipment;

import net.exmo.exmodifier.api.AppliedModifierView;
import net.exmo.exmodifier.api.AttributeContribution;
import net.exmo.exmodifier.api.ExModifierApi;
import net.exmo.exworld.battle.combatant.Combatant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Read-only ExModifier snapshot for battle/equipment. Does not change combat formulas. */
public final class ExModifierBattleBridge {
    private ExModifierBattleBridge() {}

    public static void apply(Combatant actor, ServerPlayer player) {
        if (actor == null || player == null) return;
        List<AppliedModifierView> views = new ArrayList<>(ExModifierApi.modifiersOnEquipped(player));
        ItemStack main = player.getMainHandItem();
        net.exmo.exworld.inventory.PlayerBackpack backpack = net.exmo.exworld.inventory.PlayerBackpack.of(player);
        for (int i = 0; i < 2; i++) {
            ItemStack weapon = backpack.weapon(i);
            if (weapon.isEmpty()) continue;
            for (AppliedModifierView view : ExModifierApi.modifiersOn(weapon)) {
                if (views.stream().noneMatch(existing -> existing.entryId().equals(view.entryId()) && existing.level() == view.level())) views.add(view);
            }
        }
        if (!main.isEmpty()) {
            for (AppliedModifierView view : ExModifierApi.modifiersOn(main)) {
                if (views.stream().noneMatch(existing -> existing.entryId().equals(view.entryId()) && existing.level() == view.level())) {
                    views.add(view);
                }
            }
        }
        List<String> ids = views.stream().map(view -> view.entryId().toString() + "@" + view.level()).toList();
        List<String> attributes = ExModifierApi.attributesOnEquipped(player).stream()
                .map(ExModifierBattleBridge::format)
                .toList();
        List<String> elements = new ArrayList<>();
        ExModifierApi.elementsOn(player).forEach((id, amount) -> elements.add(id + "@" + amount));
        for (ItemStack stack : ExModifierApi.equipped(player)) {
            ExModifierApi.elementsOn(stack).forEach((id, amount) -> elements.add(id + "@" + amount));
        }
        actor.setModifierSnapshot(ids, attributes);
        actor.setElementSnapshot(elements);
    }

    private static String format(AttributeContribution contribution) {
        return contribution.attribute() + "=" + contribution.amount() + ":" + contribution.operation().getSerializedName();
    }
}
