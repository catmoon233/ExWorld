package com.wan.gmmod.client.render;

import com.wan.gmmod.common.item.RitualDaggerItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class RitualDaggerRenderer extends GeoItemRenderer<RitualDaggerItem> {
    public RitualDaggerRenderer() {
        super(new RitualDaggerModel());
    }
}
