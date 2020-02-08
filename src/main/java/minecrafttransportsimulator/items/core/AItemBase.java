package minecrafttransportsimulator.items.core;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
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
	 *  The return value of this method determines if  {@link #onUsed(double, double, double, boolean)}
	 *  will be called.  Used to prevent the copious amount of checks for server/client sided
	 *  interaction and removes the need for proxies as even if there's client-side code in onUsed
	 *  method the method itself won't be called on the server so we don't need to worry.
	 */
	public abstract InteractionSide getInteractionSide();
	
	/**
	 *  Called when the item is used.  This is either a left or right-click, which is defined
	 *  by the state of the rightClicked flag.  This method is only called if {@link #getInteractionSide()}
	 *  has a value not equal to {@link InteractionSide#NONE}.  In this case, this method is called on the
	 *  appropriate side.  Unlike the regular MC code, this method is called for both clicking of
	 *  blocks and clicking in the air.  In the case of blocks, the clicked values will be the point
	 *  on the block clicked.  In the case of the air, it's the player's current position.
	 */
	public void onUsed(double clickedX, double clickedY, double clickedZ, EntityPlayer player){}
	
	public enum InteractionSide{
		CLIENT(true, false),
		SERVER(false, true),
		BOTH(true, true),
		NONE(false, false);
		
		private final boolean interactClient;
		private final boolean interactServer;
		
		private InteractionSide(boolean interactClient, boolean interactServer){
			this.interactClient = interactClient;
			this.interactServer = interactServer;
		}
		
		public boolean interactOn(boolean clientSide){
			return clientSide ? interactClient : interactServer;
		}
	}
}
