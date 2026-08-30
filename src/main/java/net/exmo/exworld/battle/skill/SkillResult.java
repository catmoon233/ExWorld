package net.exmo.exworld.battle.skill;

public record SkillResult(boolean success, String reason, double amount, boolean consumeCard, boolean chargeMana) {
    public static SkillResult success(double amount) { return new SkillResult(true, "", amount, true, true); }
    public static SkillResult effect() { return new SkillResult(true, "", 0, true, true); }
    public static SkillResult effect(double amount) { return new SkillResult(true, "", amount, true, true); }
    public static SkillResult noConsume(double amount) { return new SkillResult(true, "", amount, false, false); }
    public static SkillResult staged(double amount, boolean finalStage, boolean firstStage) {
        return new SkillResult(true, "", amount, finalStage, firstStage);
    }
    public static SkillResult failure(String reason) { return new SkillResult(false, reason, 0, false, false); }
}
