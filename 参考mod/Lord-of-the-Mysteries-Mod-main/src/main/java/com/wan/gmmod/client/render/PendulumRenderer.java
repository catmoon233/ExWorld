package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.PendulumItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * 黄水晶灵摆的 GeckoLib 物品渲染器。
 */
public class PendulumRenderer extends GeoItemRenderer<PendulumItem> {
    public PendulumRenderer() {
        super(new PendulumModel());
    }
}
