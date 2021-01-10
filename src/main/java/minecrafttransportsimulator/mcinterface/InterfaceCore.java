package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.List;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;

/**Interface to the core MC system.  This class has methods for registrations
 * file locations, and other core things that are common to clients and servers.
 * Client-specific things go into {@link InterfaceClient}, rendering goes into
 * {@link InterfaceRender}.
 *
 * @author don_bruce
 */
@SuppressWarnings("deprecation")
public class InterfaceCore{
	private static final List<String> queuedLogs = new ArrayList<String>();
	
	/**
	 *  Returns the game version for this current instance.
	 */
	public static String getGameVersion(){
		return Loader.instance().getMCVersionString().substring("Minecraft ".length());
	}
	
	/**
	 *  Returns true if the mod with the passed-in modID is present.
	 */
	public static boolean isModPresent(String modID){
		return Loader.isModLoaded(modID);
	}
	
	/**
	 *  Returns the text-based name for the passed-in mod.
	 */
	public static String getModName(String modID){
		return Loader.instance().getIndexedModList().get(modID).getName();
	}
	
	/**
	 *  Returns the text-based name for the passed-in fluid.
	 */
	public static String getFluidName(String fluidID){
		return FluidRegistry.getFluid(fluidID) != null ? new FluidStack(FluidRegistry.getFluid(fluidID), 1).getLocalizedName() : "INVALID";
	}

	/**
	 *  Returns the translation of the passed-in text from the lang file.
	 *  Put here to prevent the need for referencing the MC class directly, which
	 *  will change during updates.
	 */
	public static String translate(String text){
		return  I18n.translateToLocal(text);
	}
	
	/**
	 *  Logs an error to the logging system.  Used when things don't work right.
	 */
	public static void logError(String message){
		if(MasterLoader.logger == null){
			queuedLogs.add(message);
		}else{
			MasterLoader.logger.error(message);
		}
	}
	
	/**
     * Called to send queued logs to the logger.  This is required as the logger
     * gets created during pre-init, but logs can be generated during construction.
     */
    public static void flushLogQueue(){
    	for(String log : queuedLogs){
    		logError(log);
    	}
    }
	
	/**
	 *  Returns a list of wrappers required to craft the passed-in item.
	 */
	public static List<ItemStack> parseFromJSON(AItemPack<?> item, boolean includeMain, boolean includeSub){
		List<ItemStack> stackList = new ArrayList<ItemStack>();
		String currentSubName = "";
		try{
			//Get main materials.
			if(includeMain){
		    	for(String itemText : item.definition.general.materials){
					int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					
					int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					stackList.add(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata));
				}
			}
	    	
	    	//Get subType materials, if required.
	    	if(includeSub && item instanceof AItemSubTyped){
		    	for(String itemText : ((AItemSubTyped<?>) item).getExtraMaterials()){
					int itemQty = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					
					int itemMetadata = Integer.valueOf(itemText.substring(itemText.lastIndexOf(':') + 1));
					itemText = itemText.substring(0, itemText.lastIndexOf(':'));
					stackList.add(new ItemStack(Item.getByNameOrId(itemText), itemQty, itemMetadata));
		    	}
	    	}
	    	
	    	//Return all materials.
	    	return stackList;
		}catch(Exception e){
			e.printStackTrace();
			throw new NullPointerException("ERROR: Could not parse crafting ingredients for item: " + item.definition.packID + item.definition.systemName + currentSubName + ".  Report this to the pack author!");
		}
	}
	
	/**
	 *  Returns a fake TileEntity created to allow for such a TileEntity to be used on
	 *  entities.  TE returned is based on the name passed-in.  Currently, "chest",
	 *  "furnace", and "brewing_stand" should be supported.  The idea is that such
	 *  fake TEs can be used on moving entities anywhere in the world without the
	 *  game crashing or the GUI closing out.
	 */
	public static WrapperTileEntity getFakeTileEntity(String type, WrapperWorld world, WrapperNBT data, int inventoryUnits){
		switch(type){
			case("chest") : return new WrapperTileEntity.WrapperEntityChest(world, data, inventoryUnits);
			case("furnace") : return new WrapperTileEntity.WrapperEntityFurnace(world, data);
			case("brewing_stand") : return new WrapperTileEntity.WrapperEntityBrewingStand(world, data);
			default : return null;
		}
	}
}
