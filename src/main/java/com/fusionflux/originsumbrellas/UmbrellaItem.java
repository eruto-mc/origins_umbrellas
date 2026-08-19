package com.fusionflux.originsumbrellas;

import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;

/**
 * The umbrella. Dyeable, so the canopy takes the colour and the ribs stay plain
 * (tint index 0 is dyed, anything above it renders untinted — see {@link ClientSetup}).
 */
public class UmbrellaItem extends Item implements DyeableLeatherItem {

    public UmbrellaItem(Properties properties) {
        super(properties);
    }
}
