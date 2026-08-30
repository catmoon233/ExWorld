package net.exmo.exworld.battle.weapon;

/** Coarse weapon class used by warrior skills. Unknown items do not unlock sword or heavy techniques. */
public enum WeaponFamily {
    NONE, SWORD, HEAVY;

    public static WeaponFamily of(String itemId) {
        if (itemId == null || itemId.isBlank()) return NONE;
        String id = itemId.toLowerCase();
        if (id.equals("exworld:warrior_blade") || id.contains("sword") || id.contains("blade")) return SWORD;
        if (id.contains("axe") || id.contains("mace") || id.contains("hammer") || id.contains("mallet")) return HEAVY;
        return NONE;
    }
}