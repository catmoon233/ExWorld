package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.MagmaSwordItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class MagmaSwordRenderer extends GeoItemRenderer<MagmaSwordItem> {
    public MagmaSwordRenderer() {
        super(new MagmaSwordModel());
    }
}