package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.DawnArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class DawnArmorRenderer extends GeoArmorRenderer<DawnArmorItem> {
    public DawnArmorRenderer() {
        super(new DawnArmorModel());
    }
}