package net.exmo.exworld.social;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Command guide shown by {@code /exworld help}.
 *
 * <p>When adding a command, register a line here or call {@link #topic} from the command registrar,
 * then update {@code docs/exworld-help.md}. Do not add a player-facing command without both.</p>
 */
public final class ExworldHelpCatalog {
    public record Line(String usage, String detail) {}
    public record Topic(String id, String title, List<Line> lines) {}

    private static final Map<String, Topic> TOPICS = new LinkedHashMap<>();
    private static boolean builtins;

    private ExworldHelpCatalog() {}

    public static void ensureBuiltins() {
        if (builtins) return;
        builtins = true;
        topic("help", "帮助",
                line("/exworld help", "列出主题"),
                line("/exworld help <topic>", "查看一个主题"),
                line("/exworld help all", "查看全部指令"));
        topic("chat", "附近聊天",
                line("/exworld chat", "打开附近聊天 GUI。创造或 2 级权限可改距离和垂直范围"));
        topic("world", "世界",
                line("/exworld travel <tile>", "传送到世界地块"),
                line("/exworld anchor place", "在面前放置旅途锚点。需要 2 级权限"),
                line("/exworld world pregeneration", "查看预生成状态。需要 2 级权限"),
                line("/exworld world pregeneration on|off", "开关预生成"),
                line("/exworld world group_size", "查看区块组大小"),
                line("/exworld world group_size set <chunks>", "设置区块组大小"),
                line("/exworld world groups", "打开世界组编辑器"));
        topic("npc", "都市 NPC",
                line("/exworld npc create <id>", "创建 NPC 文档。需要 2 级权限"),
                line("/exworld npc spawn <id>", "在脚下生成 NPC"),
                line("/exworld npc edit <id>", "打开 NPC 编辑器"),
                line("/exworld npc blueprint <id>", "打开 NPC 蓝图"),
                line("/exworld npc goto <id>", "传送到 NPC 的家"),
                line("/exworld npc copy <from> <to>", "复制文档"),
                line("/exworld npc despawn <id>", "清除已生成的实体"),
                line("/exworld npc remove <id>", "删除文档并清除实体"),
                line("/exworld npc wand [id]", "给予编辑杖"),
                line("/exworld npc list", "列出文档 id"));
        topic("ship", "船",
                line("/exworld ship tool", "给予造船工具。需要 2 级权限"),
                line("/exworld ship list", "列出船体模板"),
                line("/exworld ship temple", "放入神殿模板并打开编辑器"),
                line("/exworld ship edit [id]", "打开船体编辑器"),
                line("/exworld ship spawn <id>", "生成船体"),
                line("/exworld ship disassemble", "拆掉看向的船"));
        topic("dungeon", "副本",
                line("/exworld dungeon enter <id>", "进入副本"),
                line("/exworld dungeon status", "查看当前副本"),
                line("/exworld dungeon leave", "离开副本"),
                line("/exworld dungeon place", "放置副本入口。需要 2 级权限"));
        topic("monsters", "怪物包",
                line("/exworld monsters packages", "列出数据包。需要 2 级权限"),
                line("/exworld monsters reload", "热重载数据包"),
                line("/exworld monsters edit <package>", "打开数据包编辑器"),
                line("/exworld monsters enable|disable <package>", "启用或停用"),
                line("/exworld monsters move <package> <index>", "调整优先级"),
                line("/exworld monsters import <file>", "按文件名导入"),
                line("/exworld monsters export <package>", "导出数据包"));
        topic("progress", "进度与邮件",
                line("/exworld resource get <targets> <resource>", "查询资源。需要 2 级权限"),
                line("/exworld resource add|take|set <targets> <resource> <amount>", "增减或设置资源"),
                line("/exworld quest grant|revoke|reset|fail|advance <targets> <quest>", "管理任务。需要 2 级权限"),
                line("/exworld quest branch <targets> <quest> <node>", "选择任务分支"),
                line("/exworld quest status <targets>", "查看任务状态"),
                line("/exworld mail list", "查看自己的邮件"),
                line("/exworld mail claim <id>", "领取邮件附件"),
                line("/exworld mail inspect <target>", "查看他人邮箱。需要 2 级权限"));
        topic("battle", "战斗",
                line("/exworldbattle demo", "开始演示战斗"),
                line("/exworldbattle ready [true|false]", "提交准备"),
                line("/exworldbattle auto <true|false>", "开关自动战斗"),
                line("/exworldbattle escape", "尝试逃离战斗"));
        topic("party", "队伍",
                line("/party invite <player>", "邀请玩家。解密模式关闭队伍"),
                line("/party accept", "接受邀请"),
                line("/party decline", "拒绝邀请"),
                line("/party leave", "离开队伍"),
                line("/party kick <player>", "踢出队员"),
                line("/party list", "查看队伍"));
        topic("fight", "战斗调试",
                line("/fight debug on|off|status", "战斗调试开关。需要 2 级权限"),
                line("/fight debug multi_monster on|off|status", "多怪物遭遇开关"),
                line("/fight debug start [initiative|ambush|attacked]", "对准目标开始调试战"),
                line("/fight debug start <target> [initiative|ambush|attacked]", "指定目标开始调试战"),
                line("/fight debug phase next", "推进阶段"),
                line("/fight debug intro skip|replay", "跳过或重放开场"),
                line("/fight debug result open|status", "打开或查看结算"),
                line("/fight debug finish win|lose|escape|abort", "强制结束"),
                line("/fight debug warrior_blade", "给予战士之刃"),
                line("/fight debug consumables", "给予战斗消耗品"),
                line("/fight debug cards open|seed|grant_all|reset_starter|list", "卡牌调试"),
                line("/fight debug cards give <card> [amount]", "给予卡牌"),
                line("/fight debug cards deck show|clear", "查看或清空牌组"),
                line("/fight debug cards deck add|remove <card>", "编辑牌组"));
        topic("client", "仅客户端",
                line("/exworld webview", "打开 WebView2 窗口"),
                line("/exworldvoice record <id>", "录制语音模板"),
                line("/exworldvoice delete <id>", "删除语音模板"),
                line("/exworldvoice list", "列出语音模板"));
    }

    public static void topic(String id, String title, Line... lines) {
        TOPICS.put(id, new Topic(id, title, List.of(lines)));
    }

    public static Line line(String usage, String detail) {
        return new Line(usage, detail);
    }

    public static List<Topic> topics() {
        ensureBuiltins();
        return List.copyOf(TOPICS.values());
    }

    public static Topic topic(String id) {
        ensureBuiltins();
        return TOPICS.get(id);
    }

    public static List<String> ids() {
        ensureBuiltins();
        List<String> ids = new ArrayList<>(TOPICS.keySet());
        ids.add("all");
        return ids;
    }
}
