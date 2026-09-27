# 言出法随：ExWorld 语音卡牌控制

ExWorld 对 `VoiceCastAddon` 采用可选客户端兼容。没有安装 `VoiceCastAddon` 时，ExWorld 仍可正常启动；安装后，ExWorld 会使用它的录音、MFCC/DTW 模板识别能力。

## 使用

1. 安装 `VoiceCastAddon`，进入战斗后按住 `T`（可在按键设置中修改 ExWorld 的“语音选择卡牌”）。
2. 松开按键完成识别。
3. 识别到手牌中的卡牌时，只会把该卡牌设为当前选择，并让卡牌震颤、发光约 1 秒；随后仍需按原来的目标操作出牌。

录制模板可使用客户端命令：

```text
/exworldvoice record <卡牌资源ID>
```

例如：

```text
/exworldvoice record exworld:debug_card_01
/exworldvoice record exworld:end_turn
/exworldvoice record exworld:skip_turn
/exworldvoice record exworld:skip_intro
```

录制完成后，按住 ExWorld 语音键说出同一口令。也可以使用 `/exworldvoice list` 查看已加载模板，使用 `/exworldvoice delete <资源ID>` 删除模板。

## 状态与安全边界

- 卡牌只在当前玩家处于战斗、未倒地、所属阵营行动、未结束行动，并且卡牌在当前手牌且服务端判定可用时才能被语音选中。
- 法力不足、不是自己的阵营阶段、卡牌不在手牌或战斗状态不允许时，不会选中卡牌。
- `end_turn` / `skip_turn` 只提交本体已有的结束行动或跳过开场意图，并由服务端再次判定状态。
- 语音兼容不会调用 VoiceCastAddon 的直接施法 payload，也不会帮玩家自动出牌。
