package minecrafttransportsimulator.dataclasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import mcinterface.WrapperPlayer;
import minecrafttransportsimulator.guis.instances.GUIPartBench;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**Main registry class.  This class should be referenced by any class looking for
 * MTS items or blocks.  Adding new items and blocks is a simple as adding them
 * as a field; the init method automatically registers all items and blocks in the class
 * and orders them according to the order in which they were declared.
 * This calls the {@link PackParserSystem} to register any custom vehicles and parts
 * that were loaded by packs.
 * 
 * @author don_bruce
 */
public final class MTSRegistry{
	/**All registered pack items are stored in this map as they are added.  Used to sort items in the creative tab,
	 * and will be sent to packs for item registration when so asked via {@link #getItemsForPack(String)}.  May also
	 * be used if we need to lookup a registered part item.  Map is keyed by packID to allow sorting for items from 
	 * different packs, while the sub-map is keyed by the part's {@link AJSONItem#systemName}.**/
	public static TreeMap<String, LinkedHashMap<String, AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>>> packItemMap = new TreeMap<String, LinkedHashMap<String, AItemPack<? extends AJSONItem<? extends AJSONItem<?>.General>>>>();
	
	/**Maps pack items to their list of crafting ingredients.  This is used rather than the core JSON to allow for
	 * overriding the crafting materials in said JSON, and to concatenate the materials in {@link JSONVehicle}*/
	public static final Map<AItemPack<? extends AJSONItem<?>>, String[]> packCraftingMap = new HashMap<AItemPack<? extends AJSONItem<?>>, String[]>();
	
	/**Map of creative tabs for packs.  Keyed by packID.  Populated by the {@link PackParserSystem}**/
	public static final Map<String, CreativeTabPack> packTabs = new HashMap<String, CreativeTabPack>();
	
	/**
	 * This is called by packs to query what items they have registered.
	 * Used to allow packs to register their own items after core mod processing.
	 * We need to cast-down the items to the Item class as a List with type Item is what
	 * the packloader is expecting.
	 */
	public static List<Item> getItemsForPack(String packID){
		List<Item> items = new ArrayList<Item>();
		for(AItemPack<? extends AJSONItem<?>> packItem : packItemMap.get(packID).values()){
			items.add(packItem);
		}
		return items;
	}
	
	/**
	 * This method returns a list of ItemStacks that are required
	 * to craft the passed-in pack item.  Used by {@link GUIPartBench}
	 * amd {@link WrapperPlayer#hasMaterials(AItemPack)} as well as any other systems that 
	 * need to know what materials make up pack items.
	 */
    public static List<ItemStack> getMaterials(AItemPack<? extends AJSONItem<?>> item){
    	final List<ItemStack> materialList = new ArrayList<ItemStack>();
		try{
	    	for(String itemText : MTSRegistry.packCraftingMap.get(item)){
				int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
				itemText = itemText.substring(0, itemText.lastIndexOf(':'));
				
				int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
				itemText = itemText.substring(0, itemText.lastIndexOf(':'));
				materialList.add(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata));
			}
		}catch(Exception e){
			throw new NullPointerException("ERROR: Could not parse crafting ingredients for item: " + item.definition.packID + item.definition.systemName + ".  Report this to the pack author!");
		}
    	return materialList;
    }
}
