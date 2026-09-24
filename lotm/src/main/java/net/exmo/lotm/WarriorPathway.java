package net.exmo.lotm;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.exmo.lotm.sequence.AttributeGrant;
import net.exmo.lotm.sequence.PathwayDefinition;
import net.exmo.lotm.sequence.RegisterSequencesEvent;
import net.exmo.lotm.sequence.SequenceRank;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.sequence.SequenceRegistry;
import net.exmo.lotm.spell.LotmSpells;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

/** Twilight Giant pathway. Sequences 9-7 are the playable test slice; the ladder continues to Pillar. */
public final class WarriorPathway {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("lotm", "warrior");

    private WarriorPathway() {}

    @SubscribeEvent
    public static void register(RegisterSequencesEvent event) {
        register();
    }

    public static void register() {
        SequenceRegistry.register(PathwayDefinition.builder(ID, "pathway.lotm.warrior")
                .sequence(SequenceRank.SEQUENCE_9, "sequence.lotm.warrior.9", "sequence.lotm.warrior.9.intro",
                        List.of(
                                AttributeGrant.add(key(Attributes.MAX_HEALTH), 4),
                                AttributeGrant.add(key(Attributes.ATTACK_DAMAGE), 1),
                                AttributeGrant.add(key(Attributes.ARMOR), 1),
                                AttributeGrant.add(key(Attributes.MOVEMENT_SPEED), 0.005),
                                AttributeGrant.add(mana(), 20)),
                        List.of(
                                SequenceSkill.active(LotmSpells.WARRIOR_SLASH.getId(), 1, "spell.lotm.warrior_slash", "spell.lotm.warrior_slash.guide"),
                                SequenceSkill.passive(WarriorPassives.INSTINCT, "passive.lotm.battle_instinct", "passive.lotm.battle_instinct.desc", "minecraft:iron_sword")))
                .sequence(SequenceRank.SEQUENCE_8, "sequence.lotm.warrior.8", "sequence.lotm.warrior.8.intro",
                        List.of(
                                AttributeGrant.add(key(Attributes.MAX_HEALTH), 2),
                                AttributeGrant.add(key(Attributes.ATTACK_DAMAGE), 1),
                                AttributeGrant.add(key(Attributes.ATTACK_SPEED), 0.2),
                                AttributeGrant.add(key(Attributes.KNOCKBACK_RESISTANCE), 0.15)),
                        List.of(
                                SequenceSkill.active(LotmSpells.PUGILIST_COMBO.getId(), 1, "spell.lotm.pugilist_combo", "spell.lotm.pugilist_combo.guide"),
                                SequenceSkill.passive(WarriorPassives.PRESSURE, "passive.lotm.close_pressure", "passive.lotm.close_pressure.desc", "minecraft:iron_axe")))
                .sequence(SequenceRank.SEQUENCE_7, "sequence.lotm.warrior.7", "sequence.lotm.warrior.7.intro",
                        List.of(
                                AttributeGrant.add(key(Attributes.MAX_HEALTH), 4),
                                AttributeGrant.add(key(Attributes.ATTACK_DAMAGE), 2),
                                AttributeGrant.add(key(Attributes.ARMOR_TOUGHNESS), 1),
                                AttributeGrant.add(key(Attributes.ATTACK_SPEED), 0.1)),
                        List.of(
                                SequenceSkill.active(LotmSpells.WEAPON_BREAK.getId(), 1, "spell.lotm.weapon_break", "spell.lotm.weapon_break.guide"),
                                SequenceSkill.passive(WarriorPassives.READ, "passive.lotm.read_the_blow", "passive.lotm.read_the_blow.desc", "minecraft:shield")))
                .sequence(SequenceRank.SEQUENCE_6, "sequence.lotm.warrior.6", "sequence.lotm.warrior.6.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_5, "sequence.lotm.warrior.5", "sequence.lotm.warrior.5.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_4, "sequence.lotm.warrior.4", "sequence.lotm.warrior.4.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_3, "sequence.lotm.warrior.3", "sequence.lotm.warrior.3.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_2, "sequence.lotm.warrior.2", "sequence.lotm.warrior.2.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_1, "sequence.lotm.warrior.1", "sequence.lotm.warrior.1.intro", List.of(), List.of())
                .sequence(SequenceRank.SEQUENCE_0, "sequence.lotm.warrior.0", "sequence.lotm.warrior.0.intro", List.of(), List.of())
                .sequence(SequenceRank.OLD_ONE, "sequence.lotm.warrior.old_one", "sequence.lotm.warrior.old_one.intro", List.of(), List.of())
                .sequence(SequenceRank.PILLAR, "sequence.lotm.warrior.pillar", "sequence.lotm.warrior.pillar.intro", List.of(), List.of())
                .build());
    }

    private static ResourceLocation key(Holder<Attribute> holder) {
        return holder.unwrapKey().orElseThrow().location();
    }

    private static ResourceLocation mana() {
        return AttributeRegistry.MAX_MANA.getKey().location();
    }
}
