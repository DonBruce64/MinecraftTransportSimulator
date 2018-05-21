package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import net.minecraft.item.Item;

public abstract class AItemPart extends Item{
	public final String partName;
	
	public AItemPart(String partName){
		super();
		this.partName = partName;
		this.setUnlocalizedName(partName.substring(partName.indexOf(':') + 1));
		this.setCreativeTab(MTSRegistry.packTabs.get(partName.substring(0, partName.indexOf(':'))));
	}
}
