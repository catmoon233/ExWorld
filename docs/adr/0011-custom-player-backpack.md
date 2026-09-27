# ADR 0011：自研玩家背包格网

ExWorld 需要现代风玩家物品栏：9×6 占位格网、储藏核心扩展、武器横槽与饰品原地切换。PetiteInventory 与 Item-Rarity 仅作外观与交互对照，二者均为 AGPL-3.0，源码与贴图不打进发行包。

占位、堆叠、旋转与准入在 `net.exmo.exworld.inventory` 自研；格子品质底色复用 `RarityPalette` / ExModifier `qualityOn`，与 ADR 0010 tooltip 同源。生存模式拦截原版 `InventoryScreen`，创造物品选择器保留。占位编辑器由 `/exworld inventory edit` 打开。饰品页在 Curios 存在时读取玩家当前栏位和图标，不把可变栏位写进容器菜单。
