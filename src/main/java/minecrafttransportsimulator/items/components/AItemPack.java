package minecrafttransportsimulator.items.components;

import java.util.List;

import mcinterface.WrapperNBT;
import minecrafttransportsimulator.jsondefs.AJSONItem;

/**Base item class for all pack-created items.  Stores information such as the
 * pack the item belongs to and the class that extends {@link AJSONItem} that
 * is the instance of the item's pack.  It will also attempt to place blocks
 * from this item should the pack item implement {@link IItemBlock}, and may
 * interact with vehicles should it extend {@link IItemVehicleInteractable}.
 * 
 * @author don_bruce
 */
@Deprecated//TODO DO NOT USE UNTIL AFTER V19!
public abstract class AItemPack<JSONDefinition extends AJSONItem<? extends AJSONItem<?>.General>> extends AItemBase{
	public final JSONDefinition definition;
	
	public AItemPack(JSONDefinition definition){
		super();
		this.definition = definition;
	}
	
	@Override
	public String getItemName(){
        return definition.general.name != null ? definition.general.name : definition.systemName;
	}
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, WrapperNBT data){
		if(definition.general.description != null){
			for(String tooltipLine : definition.general.description.split("\n")){
				tooltipLines.add(tooltipLine);
			}
		}
	}
	
	@Override
	public String getCreativeTabID(){
		return definition.packID;
	}
	
	/**
	 *  Returns the location of the OBJ model for this item, or null if we use the default item rendering system.
	 */
	public abstract String getModelLocation();
	
	/**
	 *  Returns the location of the texture for this item, or null if we use the default item rendering system.
	 */
	public abstract String getTextureLocation();
}
