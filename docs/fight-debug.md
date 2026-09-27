# Fight Debug 模式

Fight Debug 用于在开发环境快速验证“接战 → 战斗维度 → 卡牌行动 → 结算返回”的闭环。指令需要权限等级 2。

## 快速开始

1. `/fight debug on`：开启当前玩家的攻击遭遇监听，同时初始化 36 张测试卡和一套 12 张测试牌组。
2. 主动攻击敌对生物会触发“偷袭”，玩家阵营先行动。
3. 先被敌对生物攻击会触发“遇袭”，敌方阵营先行动。
4. `/fight debug off`：关闭攻击遭遇监听；已开始的战斗不受影响。

也可看向 32 格内的敌对生物执行 `/fight debug start`。没有目标时会生成训练僵尸。其变体：

- `/fight debug start initiative`：按先攻排序。
- `/fight debug start ambush`：模拟玩家偷袭。
- `/fight debug start attacked`：模拟玩家遇袭。
- `/fight debug start <target> [initiative|ambush|attacked]`：指定实体。

## 卡牌与牌组

- `/fight debug cards seed`：补齐 `exworld:debug_card_01` 至 `exworld:debug_card_36`，并在牌组不合法时装入前 12 张。
- `/fight debug cards list`：列出全部测试卡 ID。
- `/fight debug cards give <card> [amount]`：增加永久库存；`card` 可用 `1` 至 `36` 的简写。
- `/fight debug cards deck show|clear`：查看或清空牌组。
- `/fight debug cards deck add|remove <card>`：调整下一场战斗使用的牌组。

牌组必须有 5–30 张；同名卡入组数量不得超过卡牌库存中的持有数量。卡牌库存与牌组保存于世界 SavedData，进入会话时复制为该战斗独有的抽牌堆、手牌和弃牌堆。

## 会话控制

- `/fight debug status`：显示模式、收藏、牌组与当前会话状态。
- `/fight debug phase next`：强制结束当前阵营阶段。
- `/fight debug finish win|lose|escape|abort`：强制结算并测试返回与清场。

36 张测试卡按近战、远程、治疗、格子目标循环分组，并穿插视线穿透与忽略地形标记，主要用于覆盖卡牌、法力、目标、距离和视线验证。
