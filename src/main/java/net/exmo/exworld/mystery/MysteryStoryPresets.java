package net.exmo.exworld.mystery;

import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.npc.data.AiSettings;
import net.exmo.exworld.npc.data.DialogBackground;
import net.exmo.exworld.npc.data.DialogScript;
import net.exmo.exworld.npc.data.NpcCatalog;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.NpcLoadout;
import net.exmo.exworld.npc.data.NpcPlace;
import net.exmo.exworld.npc.entity.UrbanNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** Playable starter NPCs. Existing DM-edited documents always win. */
public final class MysteryStoryPresets {
    private MysteryStoryPresets() {}

    public static void ensure(MinecraftServer server) {
        NpcCatalog catalog = NpcCatalog.get(server);
        add(server, catalog, "priest_hand", Era.PAST, 3, 0, "神父之手", "我记得某些事，但还需要证据。",
                List.of(new Choice("检举仪式", "report", "给我一轮时间。"),
                        new Choice("告知死亡地点", "death_place", "我会为后人留下封印物。")));
        add(server, catalog, "cultist", Era.PAST, -4, 2, "邪教徒", "别靠近祭坛。",
                List.of(new Choice("查看笔记", "notes", "这份笔记提到了封印物。")));
        add(server, catalog, "bartender", Era.FUTURE, 4, 0, "酒馆老板", "想知道配方？先拿出诚意。",
                List.of(new Choice("购买配方", "buy_recipe", "配方给你，别告诉教会。")));
        add(server, catalog, "foreman", Era.FUTURE, -4, 0, "工头", "给我找四个能干活的黑户。",
                List.of(new Choice("接一份短工", "take_work", "干得好，这里是工钱。"),
                        new Choice("报告四名工人", "four_workers", "配方是你们的了。")));
        add(server, catalog, "church_staff", Era.FUTURE, 0, 5, "教会人员", "入教后才可以借封印物。",
                List.of(new Choice("加入教会", "join_church", "拿好封印物，别弄丢。")));
        add(server, catalog, "passerby", Era.FUTURE, 0, -5, "路人", "外乡人，你在找什么？",
                List.of(new Choice("说出全部真相", "reveal_all", "神降了！"), new Choice("敷衍过去", "brush_off", "我会盯着你。")));
        add(server, catalog, "black_market", Era.FUTURE, -7, -5, "黑市商人", "货是真的，来路就别问了。",
                List.of(new Choice("购买封印碎片（12 金币）", "buy_seal", "货会送到列车上。")));
    }

    /** The train is only an ending stage, so its conductor appears after players arrive. */
    public static void ensureConductor(MinecraftServer server) {
        String id = "exworld:conductor";
        if (present(server, id)) return;
        ServerPlayer anchor = server.getPlayerList().getPlayers().stream().filter(player -> {
            MysterySave.Participant p = MysterySave.get(server).participant(player.getUUID());
            return p != null && p.state == PlayerState.ENDING_CHASE;
        }).findFirst().orElse(null);
        if (anchor == null) return;
        double angle = Math.toRadians(anchor.getYRot());
        double x = anchor.getX() - Math.sin(angle) * 12;
        double y = anchor.getY();
        double z = anchor.getZ() + Math.cos(angle) * 12;
        NpcCatalog catalog = NpcCatalog.get(server);
        if (catalog.document(id).isEmpty()) {
            DialogScript.DialogButton button = new DialogScript.DialogButton("按仪式书完成封印", "perform_ritual", "");
            DialogScript talk = new DialogScript("talk", DialogScript.DialogMode.FIXED,
                    List.of("到车头来。我的灵魂只能再撑两分钟。"), List.of(button),
                    DialogBackground.DEFAULT, "", "到车头来。", 1);
            DialogScript reply = new DialogScript("perform_ritual", DialogScript.DialogMode.FIXED,
                    List.of("现在，摆好封印。"), List.of(), DialogBackground.DEFAULT, "", "现在，摆好封印。", 1);
            NpcDocument doc = new NpcDocument(id, "列车长", "", "home", "", "talk", true,
                    List.of(new NpcPlace("home", x, y, z, anchor.serverLevel().dimension().location().toString(), 0, 2)),
                    List.of(), List.of(), List.of(), List.of(talk, reply), List.of(), List.of(),
                    AiSettings.DEFAULT, NpcLoadout.EMPTY);
            catalog.put(doc, List.of());
        }
        UrbanNpc npc = ExWorldContent.URBAN_NPC.get().create(anchor.serverLevel());
        if (npc == null) return;
        npc.setDocumentId(id);
        npc.setCustomName(Component.literal("列车长残影"));
        npc.getPersistentData().putUUID("exworld_mystery_run", MysterySave.get(server).runId);
        npc.moveTo(x, y, z, anchor.getYRot(), 0);
        anchor.serverLevel().addFreshEntity(npc);
        anchor.serverLevel().sendParticles(ParticleTypes.END_ROD, x, y + 1, z, 36, .5, .8, .5, .02);
        MysteryGame.spawnEndingThreat(server, MysterySave.get(server));
    }

    private record Choice(String label, String id, String reply) {}

    private static void add(MinecraftServer server, NpcCatalog catalog, String path, Era era,
                            int dx, int dz, String name, String intro, List<Choice> choices) {
        String id = "exworld:" + path;
        NpcDocument doc = catalog.document(id).orElse(null);
        if (doc == null) {
            ServerPlayer anchor = server.getPlayerList().getPlayers().stream().filter(player -> {
                MysterySave.Participant p = MysterySave.get(server).participant(player.getUUID());
                return p != null && p.era == era;
            }).findFirst().orElse(null);
            if (anchor == null) return;
            double x = anchor.getX() + dx, y = anchor.getY(), z = anchor.getZ() + dz;
            String dim = anchor.serverLevel().dimension().location().toString();
            List<DialogScript.DialogButton> buttons = new ArrayList<>();
            List<DialogScript> dialogs = new ArrayList<>();
            for (Choice choice : choices) {
                buttons.add(new DialogScript.DialogButton(choice.label, choice.id, ""));
                dialogs.add(new DialogScript(choice.id, DialogScript.DialogMode.FIXED, List.of(choice.reply),
                        List.of(), DialogBackground.DEFAULT, "", choice.reply, 1));
            }
            dialogs.add(new DialogScript("talk", path.equals("passerby") ? DialogScript.DialogMode.AI : DialogScript.DialogMode.FIXED,
                    List.of(intro), buttons, DialogBackground.DEFAULT,
                    "你是" + name + "。只说简短、含糊的世界观台词，不承诺任何游戏结果。", intro, 1));
            doc = new NpcDocument(id, name, "", "home", "", "talk", true,
                    List.of(new NpcPlace("home", x, y, z, dim, 0, 2)), List.of(), List.of(),
                    List.of(), dialogs, List.of(), List.of(), AiSettings.DEFAULT, NpcLoadout.EMPTY);
            catalog.put(doc, List.of());
        }
        if (present(server, id)) return;
        NpcPlace home = doc.place(doc.homePlaceId()).orElse(null);
        if (home == null) return;
        ResourceLocation dimension = ResourceLocation.tryParse(home.dimension());
        ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        if (level == null) return;
        UrbanNpc npc = ExWorldContent.URBAN_NPC.get().create(level);
        if (npc == null) return;
        npc.setDocumentId(id);
        npc.moveTo(home.x(), home.y(), home.z(), (float)home.yaw(), 0);
        level.addFreshEntity(npc);
    }

    private static boolean present(MinecraftServer server, String id) {
        for (ServerLevel level : server.getAllLevels())
            for (Entity entity : level.getAllEntities())
                if (entity instanceof UrbanNpc npc && id.equals(npc.documentId())) return true;
        return false;
    }
}
