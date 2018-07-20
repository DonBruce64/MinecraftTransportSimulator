package minecrafttransportsimulator.items.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.dataclasses.PackPartObject;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.Item;

public abstract class AItemPart extends Item{
	public final String partName;
	
	public AItemPart(String partName){
		super();
		this.partName = partName;
		this.setUnlocalizedName(partName.substring(partName.indexOf(':') + 1));
		this.setCreativeTab(MTSRegistry.packTabs.get(partName.substring(0, partName.indexOf(':'))));
	}
	
	public boolean isPartValidForPackDef(PackPart packPart){
		PackPartObject itemPack = PackParserSystem.getPartPack(partName);
		return packPart.customTypes == null ? true : packPart.customTypes.contains(itemPack.general.customType);
	}
}
