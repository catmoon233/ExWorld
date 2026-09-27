# RimWorld 世界生成、World Map 与 Local Map 设计研究

> 研究日期：2026-08-12  
> 研究范围：RimWorld 的有限球形世界、world tile 与局部地图的关系、据点/区域的 tile 占用方式，以及世界地图上的地形、生物群系、道路和据点表达。  
> 标记约定：**事实**仅复述来源能直接支持的内容；**ExWorld 推论**是基于这些事实提出的设计建议，不代表 RimWorld 的实现事实。

## 结论摘要

1. RimWorld 的世界层是一个有限、可旋转的球面拓扑：球面由六边形为主、少量五边形补缝的 tile 构成。它不是把一张无限平面地图视觉上贴到球体上。
2. world tile 更接近“战略位置 + 局部地图生成条件”，而不是局部地图的等比例缩略块。生物群系、海拔、降雨、山地程度、河流、道路、地标等世界层属性会约束进入该 tile 后的局部地图，但局部地图有独立尺寸和独立生命周期。
3. RimWorld 的据点在世界层以单个 tile 上的世界对象表示；没有找到原版据点以连续多 tile 几何占地的证据。它用邻接禁建、距离外交惩罚、道路选址倾向等规则表达据点的空间影响。
4. 世界地图采用分层表达：tile 地貌/生物群系纹理是底图，道路与河流是跨 tile 的线性网络，据点、地标、车队和任务地点是高显著度图标；详细数值放在选中后的 inspect pane，而不是全部挤在地图表面。

## 1. 有限球形世界如何表示

### 已确认事实

- **事实：球面是世界模型本身。** Alpha 16 官方发布说明称世界地图改为球体，并由六边形和少量五边形覆盖；昼夜光照与当地时间对应，因此还引入了时区。[Ludeon：Alpha 16 – Wanderlust](https://ludeon.com/blog/2016/12/rimworld-alpha-16-wanderlust-released/)
- **事实：五边形是球面网格中的正常 tile，不是特殊玩法区域。** Wiki 记录世界共有 12 个五边形；除形状用于无缝覆盖球面外，其功能与普通 tile 相同。[RimWorld Wiki：World generation（Misc）](https://rimworldwiki.com/wiki/World_generation#Misc)
- **事实（源码级二手核对）：网格实现是有限邻接图。** 社区反编译重建显示 `WorldGrid`/`PlanetLayer` 从二十面体细分网格构造 tile，只接受 5 或 6 个顶点的 tile，并显式计算相邻关系。该仓库不是 Ludeon 官方发布的源码，故这里只把它作为官方描述的技术交叉验证。[RimWorldDecompiled：WorldGrid.cs](https://github.com/Chillu1/RimWorldDecompiled/blob/master/RimWorld.Planet/WorldGrid.cs)、[RimWorldDecompiled：PlanetLayer.cs](https://github.com/Chillu1/RimWorldDecompiled/blob/master/RimWorld.Planet/PlanetLayer.cs)
- **事实：生成覆盖率是可调的性能/规模参数。** 世界生成可选择 30%、50%、100% globe coverage（开发模式另有 5%），更高覆盖率会增加生成时间，并可能影响低性能机器。[RimWorld Wiki：World generation（Create world）](https://rimworldwiki.com/wiki/World_generation#Create_world)
- **事实：球面位置进入规则计算。** tile 的纬度影响温度范围与日照，海拔也影响温度；南北半球季节相反，赤道和极地表现不同。[RimWorld Wiki：World generation（Features）](https://rimworldwiki.com/wiki/World_generation#Features)

### 对 ExWorld 的设计推论

- **ExWorld 推论：先定义有限世界的拓扑，再决定画成什么。** 若玩法只需要一个有限、可达、有边界的 7×7 世界，方格网本身可以成立；若 UI 要宣称是无缝球体，则数据层应使用真正的球面邻接图，而不能只对平面坐标做纬度压缩来制造球感。
- **ExWorld 推论：战略坐标不应成为唯一身份。** 应让 `tileId` 保持稳定，并把 `mapX/mapZ` 或球面坐标视为显示/定位属性；道路、区域和旅行应引用 tile ID 与邻接关系。这样以后从方格换成六边形、折叠边界或球面网格时，存档中的地点身份不必重建。
- **ExWorld 推论：五边形不需要玩法特判。** 如果未来采用类似二十面体细分的球面网格，五边形仅是邻接数不同的普通节点；寻路与区域算法应读取邻接表，不应假定每个 tile 永远有固定的 4 或 6 个邻居。
- **ExWorld 推论：有限世界规模应是生成配置。** 与 RimWorld 的 coverage 类似，可把世界半径/tile 数与局部内容密度分开配置；小世界应减少战略节点数量，而不是缩小每个局部地图到不可玩的程度。

## 2. World tile 与局部地图的关系

### 已确认事实

- **事实：每个 world tile 携带一组战略/生成属性。** 选中 tile 可查看生物群系、地形、天气和生长期；更完整的属性包括海拔、纬度、降雨、温度、山地程度、道路和河流等。[RimWorld Wiki：World generation（Landing site / Features）](https://rimworldwiki.com/wiki/World_generation#Landing_site)
- **事实：局部地图尺寸独立于 world tile。** 开局高级设置单独选择本地地图尺寸，例如 200×200、250×250、300×300 cells 等；这说明一个 world tile 并非与固定数量的局部 cell 做地理等比例映射。[RimWorld Wiki：World generation（Advanced settings）](https://rimworldwiki.com/wiki/World_generation#Advanced_settings)
- **事实：world tile 的属性会投影为局部地图约束。** 山地程度决定局部地图生成多少山体；world tile 有河流时，局部地图会生成贯穿地图的流动水带，河流大小影响宽度和深浅水比例。[RimWorld Wiki：World generation（Terrain / Rivers）](https://rimworldwiki.com/wiki/World_generation#Rivers)
- **事实：跨尺度特征会保留方向或语义连续性。** 1.6 官方说明称局部河流会更好地匹配其在世界地图上的角度；官方 Alpha 17 也明确称道路和河流会在局部地图中生成。[Ludeon：Announcing Odyssey and update 1.6](https://ludeon.com/blog/2025/06/announcing-odyssey-and-update-1-6/)、[Ludeon：Alpha 17 – On the Road](https://ludeon.com/blog/2017/05/alpha-17-on-the-road-released/)
- **事实：进入一个世界事件可以临时创建局部地图。** 车队遭伏击会产生临时 local map；攻击派系基地会生成带守军和战利品的基地地图。官方还支持多个 local map 同时处于活动状态。[Ludeon：Alpha 16 – Wanderlust](https://ludeon.com/blog/2016/12/rimworld-alpha-16-wanderlust-released/)
- **事实：局部地图可由多个生成特征组合。** Odyssey 的一个 map 可同时拥有多项 feature；有些随机，有些依赖世界地图位置。混合生物群系会把一个局部地图分成两种生态区域。[Ludeon：Odyssey preview #1](https://ludeon.com/blog/2025/06/odyssey-preview-1-map-features-landmarks-and-biomes/)、[RimWorld Wiki：Landmarks（Mixed biome）](https://rimworldwiki.com/wiki/Landmarks#Mixed_biome)
- **事实：通常生成的地图对应一个 world tile，但官方也允许例外。** Pocket map 是没有对应 world tile 的地图，用于地下、迷宫等特殊空间；普通殖民地、伏击和工作地点地图通常都有对应 tile。[RimWorld Wiki：Pocket map](https://rimworldwiki.com/wiki/Pocket_map)

### 对 ExWorld 的设计推论

- **ExWorld 推论：把 `WorldTile` 定义为局部世界的“契约”，而非缩略图。** 契约至少应包含稳定 seed、主要/次要 biome、地形强度、海岸/河流/道路边的入射方向、资源倾向、地标/site 列表和局部地图状态引用。局部方块布局由契约生成，地图 UI 只呈现契约摘要。
- **ExWorld 推论：明确三类状态。** 建议区分 `(a)` 永久战略元数据、`(b)` 可重建的局部生成结果、`(c)` 玩家改造后必须持久化的局部状态。这样未访问 tile 可以轻量保存，访问后才物化完整区块，同时避免玩家建筑因重新生成而丢失。
- **ExWorld 推论：跨 tile 线性特征要保存“端口”。** 道路或河流不只应是 `tile.hasRoad`；应记录从哪条边进入、从哪条边离开、类型/宽度。局部生成器据此把线性特征接到相邻 tile，并让世界图和局部图朝向一致。
- **ExWorld 推论：特殊空间不要强塞进地表 tile。** 地下遗迹、室内副本、梦境等可采用 pocket-map 式的独立空间，并持有 `sourceTileId/sourceMapId`；这比占用一个虚假的地表坐标更清晰。

## 3. 定居点、据点和“跨多个 tile 的区域”

### 已确认事实

- **事实：派系可拥有许多基地，但基地作为世界对象落在具体 tile。** Alpha 16 引入非玩家派系的多个基地；车队抵达该基地 tile 后可交易或攻击，攻击时生成基地局部地图。[Ludeon：Alpha 16 – Wanderlust](https://ludeon.com/blog/2016/12/rimworld-alpha-16-wanderlust-released/)
- **事实：据点有 tile 级占用和邻接约束。** 玩家不能在派系基地本身或其相邻 tile 建殖民地；距派系基地 4 tiles 内定居还会产生随距离变化的关系惩罚。[RimWorld Wiki：Faction base](https://rimworldwiki.com/wiki/Faction_base)
- **事实：原版主要用“多个单 tile 基地”表达势力，而非一个基地横跨多个 tile。** 世界地图据点以单个图标出现，且对“基地所在 tile”“相邻 tile”“4 tiles 内”的规则分别处理。现有官方与 Wiki 资料未显示一个 vanilla settlement 同时占据连续多个 world tiles。[RimWorld Wiki：World generation（Settlements）](https://rimworldwiki.com/wiki/World_generation#Settlements)、[RimWorld Wiki：Faction base](https://rimworldwiki.com/wiki/Faction_base)
- **事实（源码级二手核对）：一个世界对象只有一个 tile 锚点。** 社区反编译重建中，`Settlement` 继承 `MapParent`/`WorldObject`，而 `WorldObject` 只有一个 `PlanetTile Tile` 位置并从该 tile 中心绘制；这直接支持“vanilla 一个 settlement world object 锚定单 tile”的判断。该源码链接仍按二手重建而非官方源码看待。[RimWorldDecompiled：Settlement.cs](https://github.com/Chillu1/RimWorldDecompiled/blob/master/RimWorld.Planet/Settlement.cs)、[MapParent.cs](https://github.com/Chillu1/RimWorldDecompiled/blob/master/RimWorld.Planet/MapParent.cs)、[WorldObject.cs](https://github.com/Chillu1/RimWorldDecompiled/blob/master/RimWorld.Planet/WorldObject.cs)
- **事实：多个活动殖民地会带来平衡和性能成本。** 官方支持多个 local map 和多个殖民地同时活动，但默认殖民地上限为 1，并明确说明这是出于平衡和性能考虑。[Ludeon：Alpha 16 – Wanderlust](https://ludeon.com/blog/2016/12/rimworld-alpha-16-wanderlust-released/)
- **事实：聚落与交通网互相影响。** 现代道路连接派系基地和主干路，派系定居点倾向位于道路附近；道路也影响车队速度和访客/袭击者出入局部地图的位置。[RimWorld Wiki：World generation（Roads / Settlements）](https://rimworldwiki.com/wiki/World_generation#Roads)

### 对 ExWorld 的设计推论

- **ExWorld 推论：把“据点占地”与“区域归属/影响”拆开。** `Site/Settlement` 可锚定一个主 tile 并生成一个局部地图；`Region/Territory` 则引用多个相邻 tile，承载命名、故事 seed、派系归属、税收、危险度或任务池。RimWorld 的邻接禁建和距离外交规则说明，影响范围不必等同于实体占地。
- **ExWorld 推论：大型聚落若确需多 tile，应使用一个聚合对象。** 例如 `Settlement { capitalTileId, footprintTileIds, districts }`，每个 tile 是一个区/入口，但名称、派系、进度和任务状态由聚落聚合根管理；不要复制四个互不知情的“城市图标”。
- **ExWorld 推论：先支持跨 tile 的 region，不必立刻支持跨 tile 的连续方块几何。** ExWorld 当前“一组相邻 world tiles 属于同一 Region”的方向更接近可扩展的故事/行政层；局部 Minecraft 空间仍可保持分块连续或按 tile 管控，无须承诺城市建筑在世界 tile 边界处逐方块严丝合缝。
- **ExWorld 推论：活动局部地图数量应有预算。** 可把已访问但无人驻留的 tile 冻结/卸载，只让有玩家、战斗、计时任务或后台生产需求的局部区域保持活跃，避免“多 tile 聚落”把所有区都永久 tick。

## 4. 世界地图如何呈现地形、生物群系、道路和据点

### 已确认事实

- **事实：生物群系是 tile 的基础视觉分类。** 每个 world tile 有一个主要 biome（Odyssey 边界 tile 可额外混合一个次要 biome）；Wiki 的世界总览图说明使用 biome 纹理缩小到 48×48 进行颜色编码。[RimWorld Wiki：Biomes](https://rimworldwiki.com/wiki/Biomes)、[RimWorld Wiki：World overview image](https://rimworldwiki.com/wiki/File:World_overview.png)
- **事实（源码级二手核对）：世界底图不是 local map 的实时缩略图。** 社区反编译重建的 `WorldDrawLayer_Terrain` 按 world tile 的主要 biome、海拔、邻接和 landmark mask 构造球面地形网格，不读取 local map 的 cells 或建筑；世界对象则由独立的 overlay draw layer 绘制。因此世界地图是参数化/符号化概览。[RimWorldDecompiled：WorldDrawLayer_Terrain.cs](https://github.com/Chillu1/RimWorldDecompiled/blob/master/RimWorld.Planet/WorldDrawLayer_Terrain.cs)、[WorldDrawLayer_WorldObjects.cs](https://github.com/Chillu1/RimWorldDecompiled/blob/master/RimWorld.Planet/WorldDrawLayer_WorldObjects.cs)
- **事实：地图表面保留地貌形态，详细属性放进检查面板。** 选中区域后，Terrain tab 展示 biome、各季旅行时间、岩石、海拔、降雨、平均温度、生长期和时区；1.6 还把洞穴等 feature 加入 tile inspect pane。[RimWorld Wiki：Menus（World）](https://rimworldwiki.com/wiki/Menus#World)、[Ludeon：Announcing Odyssey and update 1.6](https://ludeon.com/blog/2025/06/announcing-odyssey-and-update-1-6/)
- **事实：道路/河流是跨 tile 连续网络，并在局部地图中兑现。** 世界生成先形成大陆，再生成河流；古代道路来自浅层古代文明模拟，现代道路连接派系基地与主干路。道路有五种外观类型，河流有四种大小，并都会影响局部地图。[Ludeon：Alpha 17 – On the Road](https://ludeon.com/blog/2017/05/alpha-17-on-the-road-released/)
- **事实：据点使用“形状 + 颜色”双编码。** 外来者据点是房屋、部落据点是帐篷、海盗据点是骷髅旗；不同派系/态度再用蓝、紫、绿、黄、红等颜色区分，玩家殖民地使用青色。[RimWorld Wiki：World generation（Settlements）](https://rimworldwiki.com/wiki/World_generation#Settlements)
- **事实：重要地标使用自定义世界图图像。** Odyssey 地标在世界地图上有 custom art；多个特征组合时，primary feature 决定名称和世界地图绘制方式。因此一张 tile 不会把所有 feature 都各画一个同等级图标。[Ludeon：Odyssey preview #1](https://ludeon.com/blog/2025/06/odyssey-preview-1-map-features-landmarks-and-biomes/)、[RimWorld Wiki：Landmarks](https://rimworldwiki.com/wiki/Landmarks#Landmark-defining_features)
- **事实：世界对象与导航反馈覆盖在地形之上。** 殖民地与车队可直接点选；1.6 增加“Jump to…”以定位殖民地、车队、任务地点，并用绿色边框标出活动地图、黄色边框标出搜索结果。[Ludeon：Announcing Odyssey and update 1.6](https://ludeon.com/blog/2025/06/announcing-odyssey-and-update-1-6/)

### 对 ExWorld 的设计推论

- **ExWorld 推论：采用四层地图语义。** 从低到高建议为：`地表层（biome/海陆/山地） → 网络层（道路/河流/边界） → 对象层（聚落/地标/玩家/任务） → 交互层（选中、当前、搜索、未发现）`。每层解决一种视觉问题，避免当前 tile marker 同时承担地形、资源和据点的全部含义。
- **ExWorld 推论：地形缩略应是“分类纹理/生成预览”，不要伪装成局部地图截图。** 可以由 biome、海拔、湿度、山地强度和 seed 合成稳定的小纹理；它只表达环境气质。精确建筑、资源数量和危险度应出现在 inspect pane 或切换图层中。
- **ExWorld 推论：道路要画成边到边的线，而非普通相邻连接线。** 只有真实存在道路的 tile 边才渲染道路，并用线宽/材质区分小径、土路、石路、干道；一般 tile 邻接关系无需全部画出，否则会把地理图变成节点网络图。
- **ExWorld 推论：据点图标至少编码两条正交信息。** 图形表示类型（村落、城市、营地、遗迹、锚点），颜色/描边表示阵营或状态（友好、敌对、玩家、任务、活动地图）；不要让单一颜色同时承担地形和据点类型。
- **ExWorld 推论：同 tile 多 feature 应有主次。** 为 tile/site 设 `primaryFeature` 决定地图主图标，其他 feature 进入徽标、tooltip 或详情列表；这与 RimWorld 的地标组合规则一致，也能控制视觉噪声。
- **ExWorld 推论：发现状态应作用于信息精度。** 未发现 tile 可只显示粗略 biome/轮廓；侦察后再显示道路等级、资源、建筑与故事描述。搜索、当前所在地和可进入的活动地图应使用独立高亮，不改变底图颜色。

## 5. 建议落到 ExWorld 的最小数据边界

以下不是 RimWorld 事实，而是综合上述资料后的 ExWorld 建议模型：

```text
WorldTopology
  tiles: TileId -> WorldTile
  adjacency: TileId -> [TileEdge]

WorldTile
  id, coordinate/displayPosition, localSeed
  primaryBiome, secondaryBiome?
  elevation, rainfall, hilliness, temperatureBand
  edgeFeatures: road/river/coast + direction/type
  siteIds, regionId, discoveryState
  localMapRef/state

Region
  id, tileIds, name, factionId?, storySeed

Site / Settlement
  id, anchorTileId, footprintTileIds
  type, factionId, primaryFeature, secondaryFeatures
  localMapRef, activityState
```

其核心边界是：**tile 管地理与局部生成契约，region 管跨 tile 的叙事/行政归属，site 管地图对象和可进入地点，local map 管玩家真正游玩的方块状态。** 这既吸收了 RimWorld 的战略层/局部层分离，也保留了 ExWorld 现有多 tile Region 继续深化的空间。

## 来源可靠性说明

- 第一优先级为 Ludeon Studios 官方发布说明与开发预览。
- RimWorld Wiki 用于补充可操作规则、界面字段和版本后的现状；文中未把 Wiki 推测当成官方实现细节。
- 少量 `RimWorldDecompiled` 链接用于回答官方资料未公开的数据结构/绘制边界。它是社区反编译的源码级二手重建，不是 Ludeon 官方仓库；文中已逐条显式降级标注，且不以它单独支撑玩法事实。
- “原版未发现多 tile settlement”属于对已查官方资料和 Wiki 行为描述的谨慎归纳，不等于声称源码层绝无扩展接口。
