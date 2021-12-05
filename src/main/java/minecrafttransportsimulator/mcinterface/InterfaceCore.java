package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.MasterLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
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
	 *  Returns "INVALID" if the name does not exist.
	 */
	public static String getFluidName(String fluidID){
		return FluidRegistry.getFluid(fluidID) != null ? new FluidStack(FluidRegistry.getFluid(fluidID), 1).getLocalizedName() : "INVALID";
	}
	
	/**
	 *  Returns the fuel amount (in ticks) for the item in the passed-in stack.
	 *  Only returns the value for one item in the stack, not all items.
	 */
	public static int getFuelValue(ItemStack stack){
		return TileEntityFurnace.getItemBurnTime(stack);
	}
	
	/**
	 *  Returns the item that the passed-in item can be smelted to make, or an empty stack
	 *  if the passed-in item cannot be smelted.
	 */
	public static ItemStack getSmeltedItem(ItemStack stack){
		return FurnaceRecipes.instance().getSmeltingResult(stack);
	}
	
	/**
	 *  Returns all fluids currently in the game.
	 */
	public static Map<String, String> getAllFluids(){
		Map<String, String> fluidIDsToNames = new HashMap<String, String>();
		for(String fluidID : FluidRegistry.getRegisteredFluids().keySet()){
			fluidIDsToNames.put(fluidID, new FluidStack(FluidRegistry.getFluid(fluidID), 1).getLocalizedName());
		}
		return fluidIDsToNames;
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
			queuedLogs.add(MasterLoader.MODID.toUpperCase() + "ERROR: " + message);
		}else{
			MasterLoader.logger.error(MasterLoader.MODID.toUpperCase() + "ERROR: " + message);
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
	 *  Returns a fake TileEntity created to allow for such a TileEntity to be used on
	 *  entities.  TE returned is based on the name passed-in.  Currently, "chest",
	 *  "furnace", and "brewing_stand" should be supported.  The idea is that such
	 *  fake TEs can be used on moving entities anywhere in the world without the
	 *  game crashing or the GUI closing out.
	 */
	public static WrapperTileEntity getFakeTileEntity(String type, WrapperWorld world, WrapperNBT data, int inventoryUnits){
		switch(type){
			case("furnace") : return new WrapperTileEntity.WrapperEntityFurnace(world, data);
			case("brewing_stand") : return new WrapperTileEntity.WrapperEntityBrewingStand(world, data);
			default : return null;
		}
	}
}
