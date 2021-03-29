package minecrafttransportsimulator.jsondefs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Custom class designed to hold crafting overrides.  Split from the main config file
 * to prevent nuking the file if the config loading fails.  We can do without crafting
 * overrides just fine.  Not so for configs.
 * 
 * @author don_bruce
 */
public class JSONCraftingOverrides{
	public String comment1 = "The following section is used for overriding crafting recipes from packs for use in modpacks and servers.";
	public String comment2 = "Everything that is crafted on one of the benches can be modified here to include any item, including modded ones.";
	public String comment3 = "The format is one of [modID]:[ItemName]:[Metadata]:[Qty], with the name being the same as the in-game /give command.";
	public String comment4 = "If you need the full listing of items, set dumpCraftingConfig to true in the general config section.";
	public String comment5 = "This will overwrite this file with all craft-able items in all packs.";
	public String comment6 = "Note that their crafting recipes may vary depending on what packs are installed.";
	public Map<String, Map<String, List<String>>> overrides = populateDefaultOverrides();
	
	private static Map<String, Map<String, List<String>>> populateDefaultOverrides(){
		Map<String, Map<String, List<String>>> sampleOverrides = new LinkedHashMap<String, Map<String, List<String>>>();
		if(ConfigSystem.configObject.general.dumpCraftingConfig.value){
			for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
				if(!sampleOverrides.containsKey(packItem.definition.packID)){
					sampleOverrides.put(packItem.definition.packID, new LinkedHashMap<String, List<String>>());
				}
				if(packItem instanceof AItemSubTyped){
					List<String> materials = new ArrayList<String>();
					materials.addAll(packItem.definition.general.materials);
					materials.addAll(((AItemSubTyped<?>) packItem).getExtraMaterials());
					sampleOverrides.get(packItem.definition.packID).put(packItem.definition.systemName + ((AItemSubTyped<?>) packItem).subName, materials);
				}else{
					sampleOverrides.get(packItem.definition.packID).put(packItem.definition.systemName, packItem.definition.general.materials);
				}
			}
		}
		return sampleOverrides;
	}
}
