package minecrafttransportsimulator.packloading;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/**
 * Class that contains information about one set of materials for a pack item.
 * This is the number of materials, as well as the various items that can qualify for
 * these materials.  Used for crafting pack items.
 *
 * @author don_bruce
 */
public class PackMaterialComponent{
	public final int qty;
	public final int meta;
	public final List<ItemStack> possibleItems;
	
	private PackMaterialComponent(String itemText){
		possibleItems = new ArrayList<ItemStack>();
		if(itemText.startsWith("oredict:")){
			qty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
			itemText = itemText.substring(0, itemText.lastIndexOf(':'));
			meta = 0;
			String oreName = itemText.substring("oredict:".length());
			possibleItems.addAll(OreDictionary.getOres(oreName));
		}else{
			qty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
			itemText = itemText.substring(0, itemText.lastIndexOf(':'));
			meta = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
			itemText = itemText.substring(0, itemText.lastIndexOf(':'));
			possibleItems.add(new ItemStack(Item.getByNameOrId(itemText), qty, meta));
		}
	}
	
	/**
	 *  Returns the Material Components require to craft the passed-in item.  The return value is a list of lists.
	 *  Each list element corresponds to a single ingredient, with each list itself corresponding to the
	 *  possible ItemStacks that are valid for that ingredient.  The idea being that OreDict allows for
	 *  multiple items to be used. 
	 */
	public static List<PackMaterialComponent> parseFromJSON(AItemPack<?> item, boolean includeMain, boolean includeSub){
		List<PackMaterialComponent> components = new ArrayList<PackMaterialComponent>();
		String currentSubName = "";
		try{
			//Get main materials.
			if(includeMain){
		    	for(String itemText : item.definition.general.materials){
		    		components.add(new PackMaterialComponent(itemText));
				}
			}
	    	
	    	//Get subType materials, if required.
	    	if(includeSub && item instanceof AItemSubTyped){
		    	for(String itemText : ((AItemSubTyped<?>) item).getExtraMaterials()){
		    		components.add(new PackMaterialComponent(itemText));
		    	}
	    	}
	    	
	    	//Return all materials.
	    	return components;
		}catch(Exception e){
			e.printStackTrace();
			throw new NullPointerException("ERROR: Could not parse crafting ingredients for item: " + item.definition.packID + item.definition.systemName + currentSubName + ".  Report this to the pack author!");
		}
	}
}
