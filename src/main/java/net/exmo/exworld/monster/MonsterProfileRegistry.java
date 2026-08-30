package net.exmo.exworld.monster;

import com.google.gson.Gson;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.skill.SkillRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;

import java.io.IOException;
import java.util.*;

/** Live, server-owned profile resolver. Its immutable snapshot is copied into every new battle seed. */
public final class MonsterProfileRegistry {
    private static MonsterProfileRegistry active;
    private final MinecraftServer server; private final MonsterPackageManager packages; private final MonsterPackageSavedData saved;
    private volatile Snapshot snapshot = Snapshot.empty();
    private MonsterProfileRegistry(MinecraftServer server) { this.server=server;packages=new MonsterPackageManager(server);saved=server.overworld().getDataStorage().computeIfAbsent(MonsterPackageSavedData.FACTORY,"exworld_monster_packages"); }
    public static synchronized void install(MinecraftServer server) { active = new MonsterProfileRegistry(server); active.reload(); }
    public static synchronized void clear(MinecraftServer server) { if(active != null && active.server == server) active = null; }
    public static Optional<MonsterProfileRegistry> active() { return Optional.ofNullable(active); }
    public ResolvedMonsterProfile resolve(LivingEntity entity) { return snapshot.resolve(entity); }
    public List<MonsterPackageManager.PackageInfo> packages() { return packages.discover(); }
    public List<String> enabled() { return saved.enabled(); }
    public synchronized boolean setEnabled(String id, boolean enabled) { List<String> next=new ArrayList<>(saved.enabled());if(enabled){if(!next.contains(id))next.add(id);}else next.remove(id);saved.setEnabled(next);return reload(); }
    public synchronized boolean reorder(String id, int index) { List<String> next=new ArrayList<>(saved.enabled());if(!next.remove(id))return false;next.add(Math.max(0,Math.min(index,next.size())),id);saved.setEnabled(next);return reload(); }
    public synchronized boolean reload() {
        try { Snapshot candidate=Snapshot.load(packages,saved.enabled(), id -> net.exmo.exworld.battle.BattleSystem.isKnownSkill(id)); snapshot=candidate;return true; }
        catch(Exception error){Exworld.LOGGER.error("Monster package reload failed; keeping the previous configuration",error);return false;}
    }
    public MonsterPackageManager manager() { return packages; }
    public boolean savePackage(String id, String templatesJson, String rulesJson) {
        try {
            MonsterTemplate[] templates = new Gson().fromJson(templatesJson, MonsterTemplate[].class);
            MonsterRule[] rules = new Gson().fromJson(rulesJson, MonsterRule[].class);
            if (templates == null || rules == null) throw new IOException("editor data must be arrays");
            Snapshot.validatePackage(List.of(templates), List.of(rules), skillId -> net.exmo.exworld.battle.BattleSystem.isKnownSkill(skillId));
            packages.savePackage(id, List.of(templates), List.of(rules));
            return !saved.enabled().contains(id) || reload();
        } catch (Exception error) { Exworld.LOGGER.error("Monster editor save failed for {}", id, error); return false; }
    }
    private record AppliedRule(MonsterRule rule,Map<String,MonsterTemplate> templates,int packageOrder) {}
    private record Snapshot(List<AppliedRule> rules) {
        static Snapshot empty(){return new Snapshot(List.of());}
        static Snapshot load(MonsterPackageManager manager,List<String> enabled,java.util.function.Predicate<String> skillKnown)throws IOException{
            List<AppliedRule> rules=new ArrayList<>();for(int packageOrder=0;packageOrder<enabled.size();packageOrder++){var loaded=manager.load(enabled.get(packageOrder));for(MonsterTemplate template:loaded.templates().values())validate(template.profile(),skillKnown);for(MonsterRule rule:loaded.rules()){if(!rule.templateId().isBlank()&&!loaded.templates().containsKey(rule.templateId()))throw new IOException("unknown template "+rule.templateId()+" in "+rule.id());validate(rule.profile(),skillKnown);rules.add(new AppliedRule(rule,loaded.templates(),packageOrder));}}
            rules.sort(Comparator.comparingInt((AppliedRule value) -> value.rule().selector().kind() == MonsterSelector.Kind.ENTITY_TAG ? 0 : 1)
                    .thenComparingInt(AppliedRule::packageOrder).thenComparingInt(value->value.rule().priority()).thenComparing(value->value.rule().id()));return new Snapshot(List.copyOf(rules));
        }
        ResolvedMonsterProfile resolve(LivingEntity entity){ResolvedMonsterProfile profile=ResolvedMonsterProfile.defaultHostile();for(AppliedRule applied:rules)if(applied.rule().selector().matches(entity.getType())){MonsterRule rule=applied.rule();if(!rule.templateId().isBlank())profile=profile.apply(applied.templates().get(rule.templateId()).profile());profile=profile.apply(rule.profile());}return profile;}
        private static void validate(MonsterProfilePatch profile,java.util.function.Predicate<String> known)throws IOException{if(profile.maxHealth()!=null&&profile.maxHealth()<1)throw new IOException("max_health must be at least 1");if(profile.health()!=null&&profile.health()<0)throw new IOException("health cannot be negative");if(profile.maxMana()!=null&&profile.maxMana()<0)throw new IOException("max_mana cannot be negative");if(profile.mana()!=null&&profile.mana()<0)throw new IOException("mana cannot be negative");if(profile.manaPerPhase()!=null&&profile.manaPerPhase()<0)throw new IOException("mana_per_phase cannot be negative");if(profile.movementPoints()!=null&&(profile.movementPoints()<0||profile.movementPoints()>32))throw new IOException("movement_points is outside 0..32");if(profile.actionPoints()!=null&&(profile.actionPoints()<0||profile.actionPoints()>16))throw new IOException("action_points is outside 0..16");if(profile.initialHandSize()!=null&&(profile.initialHandSize()<0||profile.initialHandSize()>7))throw new IOException("initial_hand_size is outside 0..7");if(profile.drawPerPhase()!=null&&(profile.drawPerPhase()<0||profile.drawPerPhase()>7))throw new IOException("draw_per_phase is outside 0..7");if(profile.skills()!=null)for(var skill:profile.skills())if(!known.test(skill.id()))throw new IOException("unknown skill "+skill.id());}
        static void validatePackage(List<MonsterTemplate> templates,List<MonsterRule> rules,java.util.function.Predicate<String> known)throws IOException {
            Set<String> ids=new HashSet<>();for(MonsterTemplate template:templates){if(!ids.add(template.id()))throw new IOException("duplicate template "+template.id());validate(template.profile(),known);}
            for(MonsterRule rule:rules){if(!rule.templateId().isBlank()&&!ids.contains(rule.templateId()))throw new IOException("unknown template "+rule.templateId());validate(rule.profile(),known);}
        }
    }
}
