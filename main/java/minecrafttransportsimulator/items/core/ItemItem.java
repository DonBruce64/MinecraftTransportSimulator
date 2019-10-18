package minecrafttransportsimulator.items.core;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.item.Item;

public class ItemItem extends Item{
	public final String itemName;
	
	public ItemItem(String itemName){
		super();
		this.itemName = itemName;
		this.setUnlocalizedName(itemName.replace(":", "."));
		this.setCreativeTab(MTSRegistry.packTabs.get(itemName.substring(0, itemName.indexOf(':'))));
	}
}
