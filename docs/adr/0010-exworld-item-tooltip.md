# ADR 0010：ExWorld 自研物品提示框

ExWorld 需要统一的物品提示框，但不能把 simplytooltips、obscure-tooltips、EnhancedTooltips 或 ColorTooltips 打进发行包：Timefall License 禁止 bundling，Obscuria ARR 禁止衍生再分发。

因此客户端用 mixin 接管 `GuiGraphics.renderTooltipInternal`，自研面板、分区与滚动；图标与名称后标签按 EnhancedTooltips 的布局；稀有度边框/第二行按 ColorTooltips 的着色思路；标题槽位只参考 obscure-tooltips 的留白。ExModifier 词条以可换行小标签展示，套装用分区和明暗表示激活。参考模组仅作外观对照，源码均为 ExWorld 自研。
