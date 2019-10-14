package minecrafttransportsimulator.mcinterface;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**Class that is used by MTS for all player operations.  This provides a standard set of
 * methods for interacting with a player without using MC methods.  It also allows us
 * to save the player without having to worry about dumb re-naming of the player class.
 * This class's constructor will always take the current player object, whatever
 * that might be.  Try not to make a new one of these every tick for calculations, as
 * it will hurt RAM usage for sure.
 * 
 * @author don_bruce
 */
public class MTSPlayerInterface{
	private final EntityPlayer player;
	
	public MTSPlayerInterface(Entity player){
		this.player = (EntityPlayer) player;
	}

	/**Gets the Entity ID of a player.*/
	public int getID(){
		return player.getEntityId();
	}
	
	/**Gets the UUID of a player as a string.*/
	public String getUniqueID(){
		return player.getUniqueID().toString();
	}
	
	/**Gets the position of a player.*/
	public Point getPosition(){
		return new Point(player.posX, player.posY, player.posZ);
	}
	
	/**Gets the inventory of a player.*/
	public List<ItemStack> getInventory(){
		return player.inventory.mainInventory;
	}
	
	/**Gets the held stack of a player.*/
	public ItemStack geHeldStack(){
		return player.getHeldItemMainhand();
	}
	
	/**Returns true if the player has the passed-in item and quantity.
	 * Metadata is only checked on 1.12.2 and below.*/
	public boolean hasItems(Item item, int qty, int metadata){
		return player.inventory.hasItemStack(new ItemStack(item, qty, metadata));
	}
	
	/**Adds the ItemStack to the player's inventory.  Returns false if this can't be done.*/
	public boolean addStack(ItemStack stack){
		return player.inventory.addItemStackToInventory(stack);
	}
	
	/**Removes the qty of the specified items from the player's inventory.
	 * Metadata is only checked on 1.12.2 and below.*/
	public void removeItems(Item item, int qty, int metadata){
		player.inventory.clearMatchingItems(item, metadata, qty, null);
	}

	/**Gets the creative status of a player.*/
	public boolean creative(){
		return player.capabilities.isCreativeMode;
	}
	
	/**Gets the sneaking status of a player.*/
	public boolean sneaking(){
		return player.isSneaking();
	}
	
	/**Gets the op status of a player.*/
	public boolean isOP(){
		return player.getServer() == null || player.getServer().isSinglePlayer() || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
	}
	
	/**Gets the multiplayer instance of the player.  Used on servers for determining who to send a packet to.*/
	public EntityPlayerMP getMultiplayer(){
		return (EntityPlayerMP) player;
	}
}
