package net.exmo.exworld.battle.skill;

import net.exmo.exworld.battle.model.BattleCell;

public record SkillDefinition(String id, String nameKey, String icon, String adapterId, int manaCost,
                              int range, TargetType targetType, boolean requiresLineOfSight,
                              boolean piercesUnits, boolean ignoresTerrain, double power, int level, AiRole aiRole,
                              boolean chebyshevRange, boolean physical) {
    public enum TargetType { SELF, ALLY, ENEMY, CELL }
    public enum AiRole { ATTACK, HEAL, BUFF, CONTROL, AREA }
    public SkillDefinition(String id, String nameKey, String icon, String adapterId, int manaCost, int range,
                           TargetType targetType, boolean requiresLineOfSight, boolean piercesUnits,
                           boolean ignoresTerrain, double power, int level, AiRole aiRole) {
        this(id, nameKey, icon, adapterId, manaCost, range, targetType, requiresLineOfSight, piercesUnits,
                ignoresTerrain, power, level, aiRole, false, targetType == TargetType.ENEMY && range <= 1);
    }
    public SkillDefinition(String id, String nameKey, String icon, String adapterId, int manaCost, int range,
                           TargetType targetType, boolean requiresLineOfSight, boolean piercesUnits,
                           boolean ignoresTerrain, double power, int level) {
        this(id,nameKey,icon,adapterId,manaCost,range,targetType,requiresLineOfSight,piercesUnits,ignoresTerrain,power,level,inferRole(id,targetType));
    }
    public SkillDefinition withCostAndRange(int nextMana, int nextRange) {
        return new SkillDefinition(id, nameKey, icon, adapterId, nextMana, nextRange, targetType, requiresLineOfSight,
                piercesUnits, ignoresTerrain, power, level, aiRole, chebyshevRange, physical);
    }
    public boolean reaches(BattleCell origin, BattleCell target) {
        if (origin == null || target == null) return false;
        return chebyshevRange ? origin.distanceTo(target) <= range : origin.withinRadius(target, range);
    }
    private static AiRole inferRole(String id, TargetType target){String value=(id==null?"":id).toLowerCase(java.util.Locale.ROOT);if(value.contains("heal")||value.contains("recovery")||value.contains("blessing_of_life"))return AiRole.HEAL;if(value.contains("shield")||value.contains("fortify")||value.contains("haste")||value.contains("oakskin")||value.contains("ward"))return AiRole.BUFF;if(value.contains("root")||value.contains("slow")||value.contains("blight")||value.contains("poison")||value.contains("freeze")||value.contains("stun"))return AiRole.CONTROL;return target==TargetType.CELL?AiRole.AREA:AiRole.ATTACK;}
}