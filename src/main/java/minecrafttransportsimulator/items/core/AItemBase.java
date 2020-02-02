package minecrafttransportsimulator.items.core;

import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

/**Base item class for all MTS items.  Contains multiple methods to define the item's behavior,
 * such as display name, additional text to add to the tooltip, how the item handles left and
 * right-click usage, and so on.
 * 
 * @author don_bruce
 */
public abstract class AItemBase{
	
	/**
	 *  Returns the name of this item.  Will be displayed to the player in-game, but is NOT used
	 *  for item registration, so may change depending on item state.
	 */
	public abstract String getItemName();
	
	/**
	 *  Called when the item tooltip is being displayed.  The passed-in list will contain
	 *  all the lines in the tooltip, so add or remove lines as you see fit.  If you don't
	 *  want to add any lines just leave this method blank. NBT is passed-in to allow for
	 *  state-based tooltip lines to be added.
	 */
	public abstract void addTooltipLines(List<String> tooltipLines, NBTTagCompound tag);
	
	/**
	 *  Called when the item is used.  This is either a left or right-click, which is defined
	 *  by the state of the rightClicked flag.  If this item did something, and you don't want
	 *  the player to attempt to interact with any blocks they are clicking (or their offhand item)
	 *  return true.  Otherwise, return false and MC will act like this item doesn't do anything.
	 */
	public abstract boolean onUsed(double clickedX, double clickedY, double clickedZ, boolean rightClicked);
}
