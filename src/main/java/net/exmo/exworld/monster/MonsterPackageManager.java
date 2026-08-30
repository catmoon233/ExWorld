package net.exmo.exworld.monster;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Server-only ZIP package storage. Reload is transactional: an invalid ZIP never replaces the live registry. */
public final class MonsterPackageManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_ENTRIES = 2_048;
    private static final long MAX_UNCOMPRESSED_BYTES = 8L * 1024 * 1024;
    private final Path packages; private final Path imports; private final Path exports;

    public MonsterPackageManager(MinecraftServer server) {
        Path root = server.getWorldPath(LevelResource.ROOT); packages = root.resolve("datapacks").resolve("exworld-monsters");
        imports = root.resolve("exworld").resolve("import"); exports = root.resolve("exworld").resolve("export");
    }
    public Path packagesDirectory() { return packages; }
    public Path importsDirectory() { return imports; }
    public Path exportsDirectory() { return exports; }
    public List<PackageInfo> discover() {
        try { Files.createDirectories(packages); try (var stream = Files.list(packages)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".zip")).sorted().map(this::readInfo).toList();
        }} catch (IOException error) { return List.of(new PackageInfo("", "", "", false, error.getMessage())); }
    }
    public LoadedPackage load(String packageId) throws IOException {
        Path zip = packagePath(packageId); if (!Files.isRegularFile(zip)) throw new FileNotFoundException(packageId);
        try (ZipFile archive = new ZipFile(zip.toFile())) {
            JsonObject metadata = object(readEntry(archive, "pack.mcmeta"), "pack.mcmeta");
            PackageInfo info = metadata(metadata, packageId);
            if (!info.id().equals(packageId)) throw new IOException("pack id does not match file name: " + info.id());
            Map<String, MonsterTemplate> templates = new LinkedHashMap<>(); List<MonsterRule> rules = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = archive.entries(); int count = 0; long bytes = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement(); if (entry.isDirectory()) continue;
                if (++count > MAX_ENTRIES || !safeEntry(entry.getName())) throw new IOException("unsafe or excessive zip entries");
                long size = Math.max(0, entry.getSize()); if ((bytes += size) > MAX_UNCOMPRESSED_BYTES) throw new IOException("package is too large");
                String name = entry.getName();
                if (name.matches("data/[^/]+/exworld_monsters/templates/[^/]+\\.json")) {
                    MonsterTemplate template = parseTemplate(name, readEntry(archive, name));
                    if (templates.put(template.id(), template) != null) throw new IOException("duplicate template " + template.id());
                } else if (name.matches("data/[^/]+/exworld_monsters/rules/[^/]+\\.json")) rules.add(parseRule(name, readEntry(archive, name)));
            }
            rules.sort(Comparator.comparingInt(MonsterRule::priority).thenComparing(MonsterRule::id));
            return new LoadedPackage(info, templates, List.copyOf(rules));
        } catch (JsonParseException | IllegalArgumentException error) { throw new IOException("invalid monster package " + packageId + ": " + error.getMessage(), error); }
    }
    public void importPackage(String fileName) throws IOException {
        Path input = under(imports, fileName); if (!Files.isRegularFile(input)) throw new FileNotFoundException(fileName);
        try (ZipFile archive = new ZipFile(input.toFile())) {
            JsonObject meta = object(readEntry(archive, "pack.mcmeta"), "pack.mcmeta"); PackageInfo info = metadata(meta, "");
            Path destination = packagePath(info.id()); Files.createDirectories(destination.getParent()); Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    public Path exportPackage(String packageId) throws IOException {
        Path source = packagePath(packageId); if (!Files.isRegularFile(source)) throw new FileNotFoundException(packageId);
        Files.createDirectories(exports); Path destination = exports.resolve(source.getFileName()); Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING); return destination;
    }
    public void savePackage(String packageId, Collection<MonsterTemplate> templates, Collection<MonsterRule> rules) throws IOException {
        Path source = packagePath(packageId); LoadedPackage loaded = load(packageId); Map<String, byte[]> entries = copyEntries(source);
        entries.entrySet().removeIf(entry -> entry.getKey().contains("/exworld_monsters/templates/") || entry.getKey().contains("/exworld_monsters/rules/"));
        String namespace = "exworld";
        for (MonsterTemplate template : templates) entries.put(resourcePath("templates", template.id()), GSON.toJson(templateJson(template)).getBytes(StandardCharsets.UTF_8));
        for (MonsterRule rule : rules) entries.put(resourcePath("rules", rule.id()), GSON.toJson(ruleJson(rule)).getBytes(StandardCharsets.UTF_8));
        writeZipAtomically(source, entries); // metadata is intentionally preserved
    }
    private PackageInfo readInfo(Path path) { try { return load(stripZip(path.getFileName().toString())).info(); } catch (Exception e) { return new PackageInfo(stripZip(path.getFileName().toString()), "", "", false, e.getMessage()); } }
    private Path packagePath(String id) { return packages.resolve(filePart(id) + ".zip"); }
    private static Path under(Path root, String name) throws IOException { if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.contains("..")) throw new IOException("invalid file name"); Path result = root.resolve(name).normalize(); if (!result.startsWith(root.normalize())) throw new IOException("path traversal"); return result; }
    private static String filePart(String id) { if (id == null || !id.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("invalid package id"); return id; }
    private static String resourcePath(String kind, String id) { String[] parts=(id==null?"":id).split(":",2);if(parts.length!=2||!parts[0].matches("[a-z0-9_.-]+")||!parts[1].matches("[a-z0-9_./-]+")||parts[1].contains(".."))throw new IllegalArgumentException("invalid resource id "+id);return "data/"+parts[0]+"/exworld_monsters/"+kind+"/"+parts[1]+".json"; }
    private static String stripZip(String name) { return name.endsWith(".zip") ? name.substring(0, name.length() - 4) : name; }
    private static boolean safeEntry(String name) { return !name.startsWith("/") && !name.startsWith("\\") && !name.contains("\\") && Arrays.stream(name.split("/")).noneMatch(".."::equals); }
    private static String readEntry(ZipFile archive, String name) throws IOException { ZipEntry entry = archive.getEntry(name); if (entry == null) throw new IOException("missing " + name); try (InputStream input = archive.getInputStream(entry)) { return new String(input.readAllBytes(), StandardCharsets.UTF_8); } }
    private static JsonObject object(String json, String source) { JsonElement element = JsonParser.parseString(json); if (!element.isJsonObject()) throw new JsonParseException(source + " must be an object"); return element.getAsJsonObject(); }
    private static PackageInfo metadata(JsonObject json, String fallback) {
        JsonObject exworld = json.has("exworld") && json.get("exworld").isJsonObject() ? json.getAsJsonObject("exworld") : json;
        String id = string(exworld, "id", fallback); filePart(id); return new PackageInfo(id, string(exworld, "display_name", id), string(exworld, "version", ""), true, "");
    }
    private static MonsterTemplate parseTemplate(String path, String json) { JsonObject value = object(json, path); String id = string(value, "id", idFromPath(path)); return new MonsterTemplate(id, patch(value)); }
    private static MonsterRule parseRule(String path, String json) {
        JsonObject value = object(json, path); String id = string(value, "id", idFromPath(path)); int priority = integer(value, "priority", 0);
        MonsterSelector selector;
        if (value.has("entity_type")) selector = new MonsterSelector(MonsterSelector.Kind.ENTITY_TYPE, value.get("entity_type").getAsString());
        else if (value.has("entity_tag")) selector = new MonsterSelector(MonsterSelector.Kind.ENTITY_TAG, value.get("entity_tag").getAsString());
        else { JsonObject select = value.getAsJsonObject("selector"); selector = new MonsterSelector(MonsterSelector.Kind.valueOf(string(select, "kind", "ENTITY_TYPE").toUpperCase(Locale.ROOT)), string(select, "id", "")); }
        return new MonsterRule(id, selector, string(value, "template", ""), patch(value), priority);
    }
    private static MonsterProfilePatch patch(JsonObject value) {
        List<MonsterProfilePatch.Skill> skills = null;
        if (value.has("skills") && value.get("skills").isJsonArray()) { skills = new ArrayList<>(); for (JsonElement skill : value.getAsJsonArray("skills")) {
            if (skill.isJsonPrimitive()) skills.add(new MonsterProfilePatch.Skill(skill.getAsString(), 1)); else { JsonObject s=skill.getAsJsonObject(); skills.add(new MonsterProfilePatch.Skill(string(s,"id",""),integer(s,"star",1))); }
        }}
        return new MonsterProfilePatch(nullableString(value,"display_name"), nullableFloat(value,"max_health"), nullableFloat(value,"health"), nullableFloat(value,"max_mana"), nullableFloat(value,"mana"), nullableFloat(value,"mana_per_phase"), nullableDouble(value,"initiative"), nullableInt(value,"movement_points"), nullableInt(value,"action_points"), nullableInt(value,"initial_hand_size"), nullableInt(value,"draw_per_phase"), skills);
    }
    private static JsonObject templateJson(MonsterTemplate template) { JsonObject result = profileJson(template.profile()); result.addProperty("id", template.id()); return result; }
    private static JsonObject ruleJson(MonsterRule rule) { JsonObject result = profileJson(rule.profile()); result.addProperty("id", rule.id()); result.addProperty(rule.selector().kind() == MonsterSelector.Kind.ENTITY_TYPE ? "entity_type" : "entity_tag", rule.selector().id()); if (!rule.templateId().isBlank()) result.addProperty("template", rule.templateId()); result.addProperty("priority", rule.priority()); return result; }
    private static JsonObject profileJson(MonsterProfilePatch patch) { JsonObject r=new JsonObject(); add(r,"display_name",patch.displayName());add(r,"max_health",patch.maxHealth());add(r,"health",patch.health());add(r,"max_mana",patch.maxMana());add(r,"mana",patch.mana());add(r,"mana_per_phase",patch.manaPerPhase());add(r,"initiative",patch.initiative());add(r,"movement_points",patch.movementPoints());add(r,"action_points",patch.actionPoints());add(r,"initial_hand_size",patch.initialHandSize());add(r,"draw_per_phase",patch.drawPerPhase());if(patch.skills()!=null){JsonArray skills=new JsonArray();for(var skill:patch.skills()){JsonObject x=new JsonObject();x.addProperty("id",skill.id());x.addProperty("star",skill.star());skills.add(x);}r.add("skills",skills);}return r; }
    private static void add(JsonObject object,String key,Object value){if(value==null)return;if(value instanceof Number number)object.addProperty(key,number);else object.addProperty(key,String.valueOf(value));}
    private static String idFromPath(String path) { String[] parts=path.split("/"); String namespace=parts[1]; String file=parts[parts.length-1]; return namespace+":"+file.substring(0,file.length()-5); }
    private static String string(JsonObject object,String key,String fallback){return object!=null&&object.has(key)?object.get(key).getAsString():fallback;} private static int integer(JsonObject o,String k,int f){return o.has(k)?o.get(k).getAsInt():f;}
    private static String nullableString(JsonObject o,String k){return o.has(k)?o.get(k).getAsString():null;} private static Float nullableFloat(JsonObject o,String k){return o.has(k)?o.get(k).getAsFloat():null;} private static Double nullableDouble(JsonObject o,String k){return o.has(k)?o.get(k).getAsDouble():null;} private static Integer nullableInt(JsonObject o,String k){return o.has(k)?o.get(k).getAsInt():null;}
    private static Map<String,byte[]> copyEntries(Path source) throws IOException { Map<String,byte[]> result=new TreeMap<>(); try(ZipFile zip=new ZipFile(source.toFile())){Enumeration<? extends ZipEntry> entries=zip.entries();while(entries.hasMoreElements()){ZipEntry entry=entries.nextElement();if(!entry.isDirectory())try(InputStream in=zip.getInputStream(entry)){result.put(entry.getName(),in.readAllBytes());}}}return result;}
    private static void writeZipAtomically(Path target,Map<String,byte[]> entries)throws IOException{Path temp=Files.createTempFile(target.getParent(),"monster-pack-",".zip");try(ZipOutputStream zip=new ZipOutputStream(Files.newOutputStream(temp))){for(var e:entries.entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue());zip.closeEntry();}}try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}
    public record PackageInfo(String id,String displayName,String version,boolean valid,String error) {}
    public record LoadedPackage(PackageInfo info,Map<String,MonsterTemplate> templates,List<MonsterRule> rules) { public LoadedPackage { templates=Map.copyOf(templates);rules=List.copyOf(rules); } }
}
