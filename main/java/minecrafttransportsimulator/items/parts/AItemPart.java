package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.Item;

public abstract class AItemPart extends Item{
	public final String partName;
	public final String partType;
	
	public AItemPart(String partName){
		super();
		this.partName = partName;
		this.partType = PackParserSystem.getPartPack(partName).general.type;
		this.setUnlocalizedName(partName.substring(partName.indexOf(':') + 1));
		this.setCreativeTab(MTSRegistry.packTabs.get(partName.substring(0, partName.indexOf(':'))));
	}
	
	public abstract boolean isPartValueInRange(float minValue, float maxValue);
}
