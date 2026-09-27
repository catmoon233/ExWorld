package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.SilenceGunItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SilenceGunRenderer extends GeoItemRenderer<SilenceGunItem> {
    public SilenceGunRenderer() {
        super(new SilenceGunModel());
    }
}
