package net.exmo.exworld.battle.item;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.api.BattleCommand;
import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.battle.combatant.Combatant;

import java.util.List;

/** Server-side adapter for host inventory/equipment state. */
public interface BattleItemAccess {
    String useItem(BattleSession session, Combatant actor, BattleCommand.UseItem command);
    String switchWeapon(BattleSession session, Combatant actor, BattleCommand.SwitchWeapon command);
    List<BattleSnapshot.ItemView> items(Combatant actor);
    List<BattleSnapshot.WeaponSlotView> weaponSlots(BattleSession session, Combatant actor);

    static BattleItemAccess unavailable() {
        return new BattleItemAccess() {
            @Override public String useItem(BattleSession session, Combatant actor, BattleCommand.UseItem command) { return "battle.command.item_unavailable"; }
            @Override public String switchWeapon(BattleSession session, Combatant actor, BattleCommand.SwitchWeapon command) { return "battle.command.weapon_unavailable"; }
            @Override public List<BattleSnapshot.ItemView> items(Combatant actor) { return List.of(); }
            @Override public List<BattleSnapshot.WeaponSlotView> weaponSlots(BattleSession session, Combatant actor) { return List.of(); }
        };
    }
}
