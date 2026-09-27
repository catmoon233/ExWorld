# ADR 0004：Iron 法术通过白名单 adapter 接入

Iron’s Spells 的法术默认假设自由视角、连续 tick 和任意世界坐标，与格子占位和阵营阶段规则并不等价。ExWorld 只把明确声明范围、目标和安全能力的法术适配为技能；未适配法术不能进入战斗牌库，Iron 版本知识集中在 adapter 内。
