package minecrafttransportsimulator.items.components;

import java.util.List;

import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONCraftingBench;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base item class for all pack-created items.  Stores information such as the
 * pack the item belongs to and the class that extends {@link AJSONItem} that
 * is the instance of the item's pack.
 * 
 * @author don_bruce
 */
public abstract class AItemPack<JSONDefinition extends AJSONItem> extends AItemBase{
	public final JSONDefinition definition;
	private final String sourcePackID;
	
	public AItemPack(JSONDefinition definition, String sourcePackID){
		super();
		this.definition = definition;
		this.sourcePackID = sourcePackID;
	}
	
	@Override
	public String getRegistrationName(){
		return definition.packID + "." + definition.systemName;
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
		String owningPackID = definition.packID;
		String generatingPackID = sourcePackID != null ? sourcePackID : definition.packID;
		JSONPack owningConfiguration = PackParserSystem.getPackConfiguration(definition.packID);
		JSONPack generatingConfiguration = PackParserSystem.getPackConfiguration(generatingPackID);
		if(owningConfiguration.externalSkinsInOwnTab){
			return generatingPackID;
		}else if(generatingConfiguration.internalSkinsInOwnTab){
			return generatingPackID;
		}else{
			return owningPackID;
		}
	}
	
	/**
	 * Returns true if this item can be crafted by the passed-in bench definition.
	 */
	public boolean isBenchValid(JSONCraftingBench craftingDefinition){
		boolean hasMaterials = !definition.general.materials.isEmpty();
		if(!hasMaterials && this instanceof AItemSubTyped){
			AItemSubTyped<?> subTypedItem = (AItemSubTyped<?>) this;
			for(JSONSubDefinition subDefinition : subTypedItem.definition.definitions){
				if(subDefinition.subName.equals(subTypedItem.subName)){
					hasMaterials = !subDefinition.extraMaterials.isEmpty();
				}
			}
		}
		
		if(hasMaterials){
			if(craftingDefinition.items != null){
				return craftingDefinition.items.contains(definition.packID + ":" + definition.systemName);
			}else if(craftingDefinition.itemTypes.contains(definition.classification.toString().toLowerCase())){
				if(definition instanceof JSONPart && craftingDefinition.partTypes != null){
					for(String partType : craftingDefinition.partTypes){
						if(((JSONPart) definition).generic.type.contains(partType)){
							return true;
						}
					}
				}else{
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Helper method to populate default data.
	 */
	public void populateDefaultData(WrapperNBT data){
		data.setString("packID", definition.packID);
		data.setString("systemName", definition.systemName);
	}
}
