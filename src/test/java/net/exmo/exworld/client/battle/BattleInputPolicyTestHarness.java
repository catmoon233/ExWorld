package net.exmo.exworld.client.battle;

import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.skill.SkillDefinition;
import net.exmo.exworld.battle.api.BattleEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BattleInputPolicyTestHarness {
    public static void main(String[] args) {
        check(BattleInputPolicy.capturesMouse(true, false), "battle owns world clicks while no screen is open");
        check(!BattleInputPolicy.capturesMouse(true, true), "pause and settings screens keep their mouse clicks");
        check(!BattleInputPolicy.capturesMouse(false, false), "inactive battle never captures mouse input");
        gridTargetingIgnoresEntityHitboxes();
        movementPreviewRoutesAroundOccupiedCells();
        battleCameraPanIsBounded(); combatLogPresentation();
    }

    private static void gridTargetingIgnoresEntityHitboxes() {
        UUID actor = UUID.randomUUID(), vex = UUID.randomUUID(), enemy = UUID.randomUUID();
        BattleCell clickedEnemyCell = new BattleCell(3, 0, 64);
        List<BattleTargeting.Occupant> occupants = List.of(
                new BattleTargeting.Occupant(actor, new BattleCell(0, 0, 64)),
                new BattleTargeting.Occupant(vex, new BattleCell(1, 0, 64)),
                new BattleTargeting.Occupant(enemy, clickedEnemyCell));

        BattleTargeting.Intent move = BattleTargeting.move(clickedEnemyCell);
        check(move.cell().equals(clickedEnemyCell) && move.targetId() == null,
                "movement keeps the clicked battle cell even when a large entity intersects the cursor ray");
        BattleTargeting.Intent skill = BattleTargeting.skill(clickedEnemyCell, SkillDefinition.TargetType.ENEMY, actor, occupants);
        check(enemy.equals(skill.targetId()) && skill.cell().equals(clickedEnemyCell),
                "enemy card selects the combatant occupying the clicked cell instead of an off-cell ally hitbox");
        BattleTargeting.Intent cellSkill = BattleTargeting.skill(clickedEnemyCell, SkillDefinition.TargetType.CELL, actor, occupants);
        check(cellSkill.targetId() == null && cellSkill.cell().equals(clickedEnemyCell),
                "cell-targeted cards remain cell-targeted when an entity overlaps the ray");
    }

    private static void movementPreviewRoutesAroundOccupiedCells() {
        BattleCell start = new BattleCell(0, 0, 64), occupied = new BattleCell(1, 0, 64), destination = new BattleCell(2, 0, 64);
        Optional<List<BattleCell>> path = BattlePathPreview.findPath(5, start, destination, 4, List.of(occupied));
        check(path.isPresent() && path.orElseThrow().getLast().equals(destination),
                "movement preview finds a route to a free destination behind a blocking combatant");
        check(!path.orElseThrow().contains(occupied), "movement preview does not draw a route through an occupied cell");
        BattleCell interiorStart = new BattleCell(3, 3, 64), interiorDestination = new BattleCell(6, 3, 64);
        check(BattlePathPreview.findPath(10, interiorStart, interiorDestination, 3, List.of()).orElseThrow().equals(List.of(
                        new BattleCell(4, 3, 64), new BattleCell(5, 3, 64), interiorDestination)),
                "movement preview follows a direct unobstructed line");
    }

    private static void battleCameraPanIsBounded() {
        BattleCameraPan right = BattleCameraPan.move(BattleCameraPan.CENTERED, 0.0F, 0.0F, 1.0F, 18);
        BattleCameraPan left = BattleCameraPan.move(BattleCameraPan.CENTERED, 0.0F, 0.0F, -1.0F, 18);
        check(right.x() < 0 && left.x() > 0, "D and A pan to the screen's right and left respectively at the default battle yaw");
        BattleCameraPan pan = BattleCameraPan.CENTERED;
        for (int tick = 0; tick < 200; tick++) pan = BattleCameraPan.move(pan, 0.0F, 1.0F, 0.0F, 18);
        check(Math.abs(pan.x()) < .001 && pan.z() > 5.8 && pan.z() <= 6.0,
                "W moves the battle viewport camera-relative but keeps it inside the six-block bound");
        BattleCameraPan diagonal = BattleCameraPan.move(BattleCameraPan.CENTERED, 0.0F, 1.0F, 1.0F, 18);
        check(diagonal.x() < 0 && diagonal.z() > 0, "diagonal WASD input pans in the rotated camera-relative direction");
        check(BattleHud.healthNumbers(16.2F, 20.0F, 5.1F).equals("17/20 +6"),
                "player health text includes the rounded-up damage absorption value");
        check(BattleHud.healthNumbers(16.2F, 20.0F, BattleHud.hudAbsorption(0.0F, 5.1F)).equals("17/20 +6"),
                "HUD keeps showing the player's vanilla damage absorption when battle block is empty");
        check(BattleHud.hudAbsorption(3.0F, 5.1F) == 8.1F,
                "HUD combines battle block with the player's vanilla damage absorption");
    }

    private static void combatLogPresentation() {
        UUID actor = UUID.randomUUID(), target = UUID.randomUUID();
        BattleEvent first = new BattleEvent(1, 1, BattleEvent.Type.DAMAGE, actor, target, "Hero", "Vex", "test:bolt", "skill.test.bolt", 2.54);
        BattleEvent second = new BattleEvent(2, 1, BattleEvent.Type.DAMAGE, actor, target, "Hero", "Vex", "test:bolt", "skill.test.bolt", 2.54);
        BattleEvent changed = new BattleEvent(3, 1, BattleEvent.Type.DAMAGE, actor, target, "Hero", "Vex", "test:bolt", "skill.test.bolt", 2.55);
        List<BattleHud.LogLine> merged = BattleHud.mergeLogEvents(List.of(first, second, changed));
        check(merged.size() == 2 && merged.getFirst().count() == 2,
                "adjacent equal battle-log messages merge into one row with a repeat count");
        check(BattleHud.compactLog(first).getString().endsWith("-2.5"), "damage log amounts retain one decimal place");
        check(BattleHud.compactLog(merged.getFirst().event(), merged.getFirst().count()).getString().endsWith("x2"),
                "merged log rows render their xN suffix");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
