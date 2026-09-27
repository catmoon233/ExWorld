package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.DawnSwordItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class DawnSwordRenderer extends GeoItemRenderer<DawnSwordItem> {
    public DawnSwordRenderer() {
        super(new DawnSwordModel());
    }
}