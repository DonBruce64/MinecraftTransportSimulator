package minecrafttransportsimulator.wrappers;

import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;

/**Wrapper for the main player class.  This class wraps the player into a more
 * friendly instance that allows for common operations, like checking if the player
 * has an item, checking if they are OP, etc.  Also prevents the need to interact
 * with the class directly, which allows for abstraction in the code.
 *
 * @author don_bruce
 */
public class WrapperPlayer extends WrapperEntity{
	private final EntityPlayer player;
	
	public WrapperPlayer(EntityPlayer player){
		super(player);
		this.player = player;
	}
	
	/**
	 *  Returns the player's global UUID.  This is an ID that's unique to every player on Minecraft.
	 *  It does not change, ever.  Useful for assigning ownership where the entity ID of a player might
	 *  change between sessions.
	 */
	public String getUUID(){
		return EntityPlayer.getUUID(player.getGameProfile()).toString();
	}

	/**
	 *  Returns true if this player is OP.  Will always return true on single-player worlds.
	 */
	public boolean isOP(){
		return player.getServer() == null || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
	}
	
	/**
	 *  Displays the passed-in chat message to the player.  This class assumes that the message is
	 *  untranslated and will attempt to translate it prior to display.  Should this fail, the
	 *  raw message will be displayed.
	 */
	public void displayChatMessage(String message){
		Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(WrapperGUI.translate(message)));
	}
	
	/**
	 *  Returns true if this player is in creative mode.
	 */
	public boolean isCreative(){
		return player.capabilities.isCreativeMode;
	}
	
	/**
	 *  Returns true if this player is sneaking.
	 */
	public boolean isSneaking(){
		return player.isSneaking();
	}
	
	/**
	 *  Returns true if the item the player is holding is an instance of the
	 *  passed-in class.  Assumes main-hand for all cases.
	 */
	public boolean isHoldingItem(Class<?> itemClass){
		return player.getHeldItemMainhand().getItem().getClass().isInstance(itemClass);
	}
	
	/**
	 *  Returns true if the player is holding the passed-in item.
	 *  Assumes main-hand for all cases.  This method allows
	 *  for string-based checking rather than class-based.
	 */
	public boolean isHoldingItem(String itemName){
		return player.getHeldItemMainhand().getItem().equals(Item.getByNameOrId(itemName));
	}
	
	/**
	 *  Returns the held stack.
	 */
	public ItemStack getHeldStack(){
		return player.getHeldItemMainhand();
	}
	
	/**
	 *  Returns true if the player has the quantity of the passed-in item in their inventory.
	 *  Note that metadata isn't used in later MC releases.
	 */
	public boolean hasItem(Item itemToFind, int countToFind, int metadataToFind){
		for(ItemStack stack : player.inventory.mainInventory){
			if(itemToFind.equals(stack.getItem())){
				countToFind -= stack.getCount();
				if(countToFind <= 0){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 *  Attempts to add the passed-in stack to the player's inventory.
	 *  Returns true if addition was successful.
	 */
	public boolean addItem(ItemStack stackToAdd){
		return player.inventory.addItemStackToInventory(stackToAdd);
	}
	
	/**
	 *  Attempts to remove the passed-in stack from the player's inventory.
	 *  Returns true if removal was successful.  Note that if the player
	 *  is in creative mode, then removal will not actually occur.
	 */
	public boolean removeItem(ItemStack stackToRemove){
		if(isCreative()){
			return true;
		}else{
			return stackToRemove.getCount() == player.inventory.clearMatchingItems(stackToRemove.getItem(), stackToRemove.getMetadata(), stackToRemove.getCount(), stackToRemove.getTagCompound());
		}
	}
	
	/**
	 *  Sends a packet to this player over the network.
	 *  Convenience method so we don't need to call the
	 *  {@link WrapperNetwork} for player-specific packets.
	 */
	public void sendPacket(APacketBase packet){
		WrapperNetwork.sendToPlayer(packet, (EntityPlayerMP) player);
	}
}