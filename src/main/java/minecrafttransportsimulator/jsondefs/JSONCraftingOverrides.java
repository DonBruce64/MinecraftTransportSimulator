package minecrafttransportsimulator.jsondefs;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.systems.ConfigSystem;

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
	public String comment4 = "A sample override config is given here for reference.  Modify as you see fit.";
	public Map<String, Map<String, String[]>> overrides = populateDefaultOverrides();
	
	private static Map<String, Map<String, String[]>> populateDefaultOverrides(){
		Map<String, Map<String, String[]>> sampleOverrides = new HashMap<String, Map<String, String[]>>();
		if(ConfigSystem.configObject.general.dumpCraftingConfig.value){
			for(AItemPack<? extends AJSONItem<?>> item : MTSRegistry.packCraftingMap.keySet()){
				if(!sampleOverrides.containsKey(item.definition.packID)){
					sampleOverrides.put(item.definition.packID, new HashMap<String, String[]>());
				}
				sampleOverrides.get(item.definition.packID).put(item.definition.systemName, MTSRegistry.packCraftingMap.get(item));
			}
		}else{
			Map<String, String[]> packSampleOverride = new HashMap<String, String[]>();
			packSampleOverride.put("wheellarge", new String[]{"minecraft:dye:0:3", "minecraft:iron_ingot:0:3", "minecraft:wool:15:3"});
			packSampleOverride.put("engineamci4", new String[]{"minecraft:piston:0:4", "minecraft:obsidian:0:3", "minecraft:iron_ingot:0:1"});
			sampleOverrides.put("mtsofficialpack", packSampleOverride);
			
			packSampleOverride = new HashMap<String, String[]>();
			packSampleOverride.put("unuparts_decor_unu_cutout_derrick", new String[]{"minecraft:heavy_weighted_pressure_plate:0:1", "minecraft:dye:6:2"});
			sampleOverrides.put("unuparts", packSampleOverride);
		}
		return sampleOverrides;
	}
}
