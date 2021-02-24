package minecrafttransportsimulator.items.components;

import java.util.List;

import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base item class for all pack-created items.  Stores information such as the
 * pack the item belongs to and the class that extends {@link AJSONItem} that
 * is the instance of the item's pack.
 * 
 * @author don_bruce
 */
public abstract class AItemPack<JSONDefinition extends AJSONItem> extends AItemBase{
	public static final String PACKID_SEPARATOR = ".";
	public final JSONDefinition definition;
	private final String sourcePackID;
	
	public AItemPack(JSONDefinition definition, String sourcePackID){
		super();
		this.definition = definition;
		this.sourcePackID = sourcePackID;
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
		String owningPackID = definition.packID;
		String generatingPackID = sourcePackID != null ? sourcePackID : definition.packID;
		JSONPack owningConfiguration = PackParserSystem.getPackConfiguration(definition.packID);
		JSONPack generatingConfiguration = PackParserSystem.getPackConfiguration(generatingPackID);
		//TODO remove this when the old loaders are gone.
		if(owningConfiguration == null){
			return owningPackID;
		}else{
			if(owningConfiguration.externalSkinsInOwnTab){
				return generatingPackID;
			}else if(generatingConfiguration.internalSkinsInOwnTab){
				return generatingPackID;
			}else{
				return owningPackID;
			}
		}
	}
	
	/**
	 * This method ensures that the passed-in data contains everything required to
	 * create whatever this item represents.  Data is added to the passed-in data
	 * if required to ensure it contains the minimal information for the creation
	 * of the entity.  If the data block is null, a new one is returned.
	 */
	public WrapperNBT validateData(WrapperNBT data){
		if(data == null){
			data = new WrapperNBT();
		}
		if(data.getString("packID").isEmpty()){
			populateDefaultData(data);
		}
		return data;
	}
	
	/**
	 * Helper method to populate default data.
	 */
	protected void populateDefaultData(WrapperNBT data){
		data.setString("packID", definition.packID);
		data.setString("systemName", definition.systemName);
	}
}
