# ADR 0001：NeoForge 世界状态使用 SavedData adapter

## 状态

已接受。

## 背景

项目运行于 NeoForge 1.21.1，但 Cardinal Components API 6.1.x 官方仅支持 Fabric / Quilt，构件依赖 Fabric Loader 入口点与 mixin，无法作为 NeoForge 模组加载。世界状态又必须稳定落盘，并允许未来迁移实现。

## 决策

世界逻辑只依赖 `WorldStateStore` seam。当前 NeoForge adapter 使用主世界的 `SavedData` 保存全局世界状态；如果未来提供 Fabric 构建，再增加 CCA adapter，不改变世界生成、旅行或网络模块。

## 后果

- 当前 NeoForge 构建不伪装或内嵌不可加载的 CCA。
- 存档位于主世界 `data/exworld_world_state.dat`。
- 存储格式与上层世界接口分离，迁移成本受控。
