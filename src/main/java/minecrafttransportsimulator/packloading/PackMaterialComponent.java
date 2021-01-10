package minecrafttransportsimulator.packloading;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
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
			NonNullList<ItemStack> oreDictMaterials = OreDictionary.getOres(oreName, true);
			List<ItemStack> possibleMaterials = new ArrayList<ItemStack>();
			if(oreDictMaterials.isEmpty()){
				InterfaceCore.logError("ERROR: Could not obtain any materials for oredict ore name:" + oreName);
			}else{
				for(ItemStack oreDictMaterial : oreDictMaterials){
					if(oreDictMaterial.getMetadata() == OreDictionary.WILDCARD_VALUE){
						//Just get the first material here.
						//We can't loop over all valid ones as there's not a finite list anywhere.
						possibleMaterials.add(new ItemStack(oreDictMaterial.getItem(), 1));
					}else{
						possibleMaterials.add(oreDictMaterial);
					}
				}
			}
			possibleItems.addAll(possibleMaterials);
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
	 *  multiple items to be used.  If this component is not for crafting checks, set forCraftingCheck to false.
	 *  This prevents the returned stacks from having the wildcard value in their metadata and not being actual items.
	 */
	public static List<PackMaterialComponent> parseFromJSON(AItemPack<?> item, boolean includeMain, boolean includeSub, boolean forCraftingCheck){
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
