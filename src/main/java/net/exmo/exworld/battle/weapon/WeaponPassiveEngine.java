package net.exmo.exworld.battle.weapon;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.api.event.BattleEvents;
import net.neoforged.neoforge.common.NeoForge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Code-registered passive seam. Passive state remains inside the battle aggregate. */
public final class WeaponPassiveEngine {
    public static final String WARRIOR_BLADE = "exworld:warrior_blade";
    private final Map<String, WeaponDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, WeaponPassive> passives = new LinkedHashMap<>();

    public WeaponPassiveEngine() {
        // The item is only the carrier. The passive implementation is registered independently.
        register(new WeaponDefinition(WARRIOR_BLADE, "item.exworld.warrior_blade", "warrior_blade"), new WarriorBladePassive());
    }

    public void register(WeaponDefinition definition, WeaponPassive passive) {
        if (definition == null || definition.itemId().isBlank() || passive == null) return;
        definitions.put(definition.itemId(), definition);
        passives.put(definition.itemId(), passive);
    }
    /** Compatibility overload for integrations that only have an item id and passive. */
    public void register(String itemId, WeaponPassive passive) {
        register(new WeaponDefinition(itemId, itemId, passive == null ? "" : passive.id()), passive);
    }
    public WeaponPassive passive(String itemId) { return passives.get(itemId); }
    public Optional<WeaponDefinition> definition(String itemId) { return Optional.ofNullable(definitions.get(itemId)); }
    public String passiveId(String itemId) { return Optional.ofNullable(passives.get(itemId)).map(WeaponPassive::id).orElse(""); }
    public int passiveProgress(String itemId, Combatant actor) {
        WeaponPassive passive = passives.get(itemId);
        return passive == null ? 0 : passive.progress(actor);
    }
    public void onAttackCard(BattleSession session, Combatant actor) {
        WeaponPassive passive = passives.get(actor.activeWeaponItem());
        if (passive != null) {
            passive.onAttackCard(session, actor);
            NeoForge.EVENT_BUS.post(new BattleEvents.PassiveTriggered(
                    session, actor, passive.id(), "attack_card", passive.progress(actor)));
        }
    }
}
