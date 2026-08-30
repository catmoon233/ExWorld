package net.exmo.exworld.battle.item;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.model.BattleCell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative context passed to one battle consumable adapter. */
public record BattleItemUseContext(BattleSession session, Combatant actor, ServerPlayer player,
                                   ItemStack stack, BattleCell targetCell) {}
