package minecrafttransportsimulator.mcinterface;

import java.util.List;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.jsondefs.AJSONItem;

/**Interface to the core MC system.  This class has methods for registrations
 * file locations, and other core things that are common to clients and servers.
 * Client-specific things go into {@link IInterfaceGame}, rendering goes into
 * {@link IInterfaceRender}.
 *
 * @author don_bruce
 */
public interface IInterfaceCore{
	
	/**
	 *  Returns true if the mod with the passed-in modID is present.
	 */
	public boolean isModPresent(String modID);
	
	/**
	 *  Returns the text-based name for the passed-in mod.
	 */
	public String getModName(String modID);
	
	/**
	 *  Returns the text-based name for the passed-in fluid.
	 */
	public String getFluidName(String fluidID);

	/**
	 *  Returns the translation of the passed-in text from the lang file.
	 *  Put here to prevent the need for referencing the MC class directly, which
	 *  will change during updates.
	 */
	public String translate(String text);
	
	/**
	 *  Logs an error to the logging system.  Used when things don't work right.
	 */
	public void logError(String message);
	
	/**
	 *  Returns a new, empty NBT tag for use.
	 */
	public IWrapperNBT createNewTag();
	
	/**
	 *  Returns a stack containing the passed-in item.
	 */
	public IWrapperItemStack getStack(AItemBase item);
	
	/**
	 *  Returns a list of wrappers created from the JSON definition listing.
	 *  Note that while different versions of MC will reference different definition 
	 *  sections due to the "flattening" changing item names, the end result will be
	 *  a list of stacks needed to craft the passed-in item based on the definition. 
	 */
	public List<IWrapperItemStack> parseFromJSON(AJSONItem<?> packDef);
	
	/**
	 *  Returns a fake TileEntity created to allow for such a TileEntity to be used on
	 *  entities.  TE returned is based on the name passed-in.  Currently, "chest",
	 *  "furnace", and "brewing_stand" should be supported.  The idea is that such
	 *  fake TEs can be used on moving entities anywhere in the world without the
	 *  game crashing or the GUI closing out.
	 */
	public IWrapperTileEntity getFakeTileEntity(String type, IWrapperWorld world, IWrapperNBT data, int inventoryUnits);
}
