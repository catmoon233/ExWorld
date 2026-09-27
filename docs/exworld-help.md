# ExWorld 指令帮助

给后续改这个仓库的 AI agent。玩家用 `/exworld help` 看指令指南。新增、改名或删除任何玩家能执行的指令时，必须同步帮助，不能只改注册代码。

## 必须同时改的两处

1. 运行时目录：
   - 本模组指令加到 `src/main/java/net/exmo/exworld/social/ExworldHelpCatalog.java` 的 `ensureBuiltins()`。
   - 别的模组挂在 `exworld` 下的指令，在自己的 `RegisterCommandsEvent` 里调用 `ExworldHelp.topic(...)`。序列指令的例子在 `lotm/src/main/java/net/exmo/lotm/sequence/SequenceCommands.java`。
2. 本文下面的「当前指令」表。漏掉任何一处，`/exworld help` 就会和真实指令不一致。

每条帮助都要写成 `/完整命令` 加一句中文说明，并标明权限。主题 id 用小写英文，和 `/exworld help <topic>` 的参数一致。

不要把 `exworld` 根节点的 `requires(permission 2)` 加回去。怪物包的权限在 `monsters` 节点上，否则普通玩家打不开 `/exworld help`。

## 当前指令

| 主题 | 命令 | 说明 |
| --- | --- | --- |
| help | `/exworld help` | 列出主题 |
| help | `/exworld help <topic>` | 查看一个主题 |
| help | `/exworld help all` | 查看全部指令 |
| inventory | `/exworld inventory edit` | 打开物品占位配置。需要 2 级权限 |
| character | `/exworld character import <targets> <file.md>` | 导入 Markdown 作为人物介绍。文件放在运行目录、`characters/` 或 `config/exworld/characters/`。需要 2 级权限 |
| character | `/exworld character clear <targets>` | 清空人物介绍。需要 2 级权限 |
| chat | `/exworld chat` | 打开附近聊天 GUI。创造或 2 级权限可改距离和垂直范围 |
| world | `/exworld travel <tile>` | 传送到世界地块 |
| world | `/exworld anchor place` | 放置旅途锚点。需要 2 级权限 |
| world | `/exworld world pregeneration` | 查看预生成。需要 2 级权限 |
| world | `/exworld world pregeneration on\|off` | 开关预生成 |
| world | `/exworld world group_size` | 查看区块组大小 |
| world | `/exworld world group_size set <chunks>` | 设置区块组大小 |
| world | `/exworld world groups` | 打开世界组编辑器 |
| npc | `/exworld npc create <id>` | 创建文档。需要 2 级权限 |
| npc | `/exworld npc spawn <id>` | 生成 NPC |
| npc | `/exworld npc edit <id>` | 打开编辑器 |
| npc | `/exworld npc blueprint <id>` | 打开蓝图 |
| npc | `/exworld npc goto <id>` | 传送到家 |
| npc | `/exworld npc copy <from> <to>` | 复制文档 |
| npc | `/exworld npc despawn <id>` | 清除实体 |
| npc | `/exworld npc remove <id>` | 删除文档 |
| npc | `/exworld npc wand [id]` | 给予编辑杖 |
| npc | `/exworld npc list` | 列出 id |
| ship | `/exworld ship tool` | 给予造船工具。需要 2 级权限 |
| ship | `/exworld ship list` | 列出模板 |
| ship | `/exworld ship temple` | 放入神殿模板 |
| ship | `/exworld ship edit [id]` | 打开编辑器 |
| ship | `/exworld ship spawn <id>` | 生成船体 |
| ship | `/exworld ship disassemble` | 拆掉看向的船 |
| dungeon | `/exworld dungeon enter <id>` | 进入副本 |
| dungeon | `/exworld dungeon status` | 查看当前副本 |
| dungeon | `/exworld dungeon leave` | 离开副本 |
| dungeon | `/exworld dungeon place` | 放置入口。需要 2 级权限 |
| monsters | `/exworld monsters packages` | 列出数据包。需要 2 级权限 |
| monsters | `/exworld monsters reload` | 热重载 |
| monsters | `/exworld monsters edit <package>` | 打开编辑器 |
| monsters | `/exworld monsters enable\|disable <package>` | 启用或停用 |
| monsters | `/exworld monsters move <package> <index>` | 调整优先级 |
| monsters | `/exworld monsters import <file>` | 按文件名导入 |
| monsters | `/exworld monsters export <package>` | 导出 |
| progress | `/exworld resource get <targets> <resource>` | 查询资源。需要 2 级权限 |
| progress | `/exworld resource add\|take\|set <targets> <resource> <amount>` | 改资源 |
| progress | `/exworld quest grant\|revoke\|reset\|fail\|advance <targets> <quest>` | 管理任务。需要 2 级权限 |
| progress | `/exworld quest branch <targets> <quest> <node>` | 选择分支 |
| progress | `/exworld quest status <targets>` | 查看状态 |
| progress | `/exworld mail list` | 查看自己的邮件 |
| progress | `/exworld mail claim <id>` | 领取附件 |
| progress | `/exworld mail inspect <target>` | 查看他人邮箱。需要 2 级权限 |
| battle | `/exworldbattle demo` | 演示战斗 |
| battle | `/exworldbattle ready [true\|false]` | 提交准备 |
| battle | `/exworldbattle auto <true\|false>` | 自动战斗 |
| battle | `/exworldbattle escape` | 逃离 |
| party | `/party invite <player>` | 邀请。解密模式关闭队伍 |
| party | `/party accept` | 接受 |
| party | `/party decline` | 拒绝 |
| party | `/party leave` | 离开 |
| party | `/party kick <player>` | 踢出 |
| party | `/party list` | 查看队伍 |
| fight | `/fight debug on\|off\|status` | 调试开关。需要 2 级权限 |
| fight | `/fight debug multi_monster on\|off\|status` | 多怪物遭遇 |
| fight | `/fight debug start [initiative\|ambush\|attacked]` | 对准目标开战 |
| fight | `/fight debug start <target> [initiative\|ambush\|attacked]` | 指定目标开战 |
| fight | `/fight debug phase next` | 推进阶段 |
| fight | `/fight debug intro skip\|replay` | 开场 |
| fight | `/fight debug result open\|status` | 结算 |
| fight | `/fight debug finish win\|lose\|escape\|abort` | 强制结束 |
| fight | `/fight debug warrior_blade` | 给予战士之刃 |
| fight | `/fight debug consumables` | 给予消耗品 |
| fight | `/fight debug cards open\|seed\|grant_all\|reset_starter\|list` | 卡牌调试 |
| fight | `/fight debug cards give <card> [amount]` | 给予卡牌 |
| fight | `/fight debug cards deck show\|clear` | 查看或清空牌组 |
| fight | `/fight debug cards deck add\|remove <card>` | 编辑牌组 |
| client | `/exworld webview` | 仅客户端 |
| client | `/exworldvoice record <id>` | 仅客户端 |
| client | `/exworldvoice delete <id>` | 仅客户端 |
| client | `/exworldvoice list` | 仅客户端 |
| memory | `/exworld memory record <id>` | 客户端在 ReForgedPlay 录像中标记起点 |
| memory | `/exworld memory stop` | 标记终点；退出世界后封存录像 |
| memory | `/exworld memory play <id>` | 退出当前世界并播放片段 |
| memory | `/exworld memory loop <id>` | 退出当前世界并循环播放片段 |
| memory | `/exworld memory halt` | 停止回放并返回主菜单 |
| memory | `/exworld memory start <id> <秒>` | 调整录像中的起点秒数 |
| memory | `/exworld memory end <id> <秒>` | 调整录像中的终点秒数 |
| memory | `/exworld memory list` | 列出本机已封存的记忆 |
| memory | `/exworld memory delete <id>` | 删除记忆标记，保留原始录像 |
| memory | `/exworld memory status` | 查看状态 |
| sequence | `/exworld sequence open` | lotm 加载后才出现 |
| sequence | `/exworld sequence get [targets]` | 查看序列 |
| sequence | `/exworld sequence pathways` | 列出途径 |
| sequence | `/exworld sequence set <targets> <pathway> <rank>` | 需要 2 级权限 |
| sequence | `/exworld sequence clear <targets>` | 需要 2 级权限 |
| phone | `/exworld phone sim <number> [target]` | 获取指定号码的 SIM 卡。号码必须为 7 位数字。需要 2 级权限 |

## 记忆回放与 ReForgedPlay

客户端需要安装 `reforgedplaymod-1.21.1-0.3.jar`，并在进入世界前启用 ReForgedPlay 的自动录制。`record` 和 `stop` 在本机录像里写入起止标记；录像由 ReForgedPlay 在退出世界时封存到它的录像目录。`play` 和 `loop` 会先退出当前世界，等待封存完成，再用 ReForgedPlay 打开片段。结束后返回主菜单，不会自动重连服务器。

记忆索引保存在本机 `.mcpr` 的标记里。`delete` 只删除这些标记，原始录像仍可从 ReForgedPlay 中打开；旧版 ExWorld 的实体采样记忆不会自动转换为 `.mcpr`。ReForgedPlay 的录像不包含此前 ExWorld 单独缓存的 Simple Voice Chat 音频。
