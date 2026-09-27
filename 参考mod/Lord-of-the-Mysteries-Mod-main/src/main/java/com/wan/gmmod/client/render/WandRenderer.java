package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.WandItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class WandRenderer extends GeoItemRenderer<WandItem> {
    public WandRenderer() {
        super(new WandModel());
    }
}