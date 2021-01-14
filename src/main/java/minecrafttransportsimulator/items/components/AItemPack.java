package minecrafttransportsimulator.items.components;

import java.util.List;

import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base item class for all pack-created items.  Stores information such as the
 * pack the item belongs to and the class that extends {@link AJSONItem} that
 * is the instance of the item's pack.
 * 
 * @author don_bruce
 */
public abstract class AItemPack<JSONDefinition extends AJSONItem<?>> extends AItemBase{
	public static final String PACKID_SEPARATOR = ".";
	public final JSONDefinition definition;
	
	public AItemPack(JSONDefinition definition){
		super();
		this.definition = definition;
	}
	
	@Override
	public String getRegistrationName(){
		//TODO remove this when we no longer have external loaders.
		//return definition.packID + ":" + definition.systemName;
		return (PackParserSystem.getPackConfiguration(definition.packID) == null || PackParserSystem.getPackConfiguration(definition.packID).internallyGenerated) ? definition.systemName : definition.packID + PACKID_SEPARATOR + definition.systemName;
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
	public int getStackSize(){
		return definition.general.stackSize != 0 ? definition.general.stackSize : super.getStackSize();
	}
	
	@Override
	public String getCreativeTabID(){
		return definition.packID;
	}
}
