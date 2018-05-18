package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.multipart.parts.APart;
import net.minecraft.item.Item;

public abstract class AItemPart extends Item{
	public final Class<? extends APart> partClass;
	
	public AItemPart(Class<? extends APart> partClass){
		this.partClass = partClass;
		this.hasSubtypes=true;
		this.setMaxStackSize(1);
	}
}
