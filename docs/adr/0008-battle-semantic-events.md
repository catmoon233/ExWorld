# ADR 0008：战斗语义事件作为规则扩展 API

## 状态

已接受。

## 决策

战斗系统直接使用 NeoForge 的 `NeoForge.EVENT_BUS`。`BattleEvents` 中的每个语义事件都是原生 NeoForge `Event`，`BattleSession` 只负责在权威结算点发布事件；`BattleEngine.events()` 和 `BattleSystem.battleEvents()` 返回同一个 `IEventBus`，供跨战斗订阅入口使用。

事件分成两类：

- `*AboutTo*` 前置事件：可取消；伤害、治疗和状态事件还可调整即将结算的数值。
- 结果事件：只在权威状态已经改变后发布，例如 `DamageDealt`、`CardPlayed`、`MoveCompleted`、`ItemUsed` 和 `BattleOutcome`。

旧的 `BattleEvent` 保留作为快照、网络和动画展示事件，并通过 `Presentation` 事件桥接到新事件总线。

监听器使用 NeoForge 原生的 typed listener、`EventPriority` 和 `receiveCanceled` 语义。可取消事件实现 `ICancellableEvent`；监听器生命周期由 NeoForge 事件总线管理，不再维护会话级转发器、订阅对象或自定义异常隔离层。

例如，外部规则可以直接注册：

```java
NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false,
        BattleEvents.DamageAboutToBeDealt.class,
        event -> event.amount(event.amount() * 0.8D));
```

## 原因

武器被动、战斗物品、任务、成就、统计和外部模组都需要观察同一套权威战斗变化。如果各自监听 HUD 日志或修改 `BattleSession`，会形成隐式耦合并漏掉冲刺、药水、外部法术等结算路径。

## 后果

- 新规则可以在事件 seam 注册，而不用修改命令模型或客户端协议。
- 前置事件允许护盾、免疫、伤害修正和战斗限制等规则在结算前介入。
- 事件在服务端线程同步执行；监听器不能阻塞或依赖客户端输入。
- 事件运输层复用 NeoForge 原生总线，避免每个战斗会话创建和遍历额外的监听器列表。
- 事件总线不是持久化事实来源；需要恢复的状态仍必须写入战斗 NBT。
