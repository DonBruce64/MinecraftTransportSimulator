package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.packets.components.APacketBase;

/**Wrapper for the player entity class.  This class wraps the player into a more
 * friendly instance that allows for common operations, like checking if the player
 * has an item, checking if they are OP, etc.  Also prevents the need to interact
 * with the class directly, which allows for abstraction in the code.
 *
 * @author don_bruce
 */
public interface IWrapperPlayer extends IWrapperEntity{
	
	/**
	 *  Returns the player's global UUID.  This is an ID that's unique to every player on Minecraft.
	 *  Useful for assigning ownership where the entity ID of a player might change between sessions.
	 *  <br><br>
	 *  NOTE: While this ID isn't supposed to change, some systems WILL, in fact, change it.  Cracked
	 *  servers, and the nastiest of Bukkit systems will deliberately change the UUID of players, which,
	 *  when combined with their changing of entity IDs, makes server-client lookup impossible.
	 */
	public String getUUID();

	/**
	 *  Returns true if this player is OP.  Will always return true on single-player worlds.
	 */
	public boolean isOP();
	
	/**
	 *  Displays the passed-in chat message to the player.  This interface assumes that the message is
	 *  untranslated and will attempt to translate it prior to display.  Should this fail, the
	 *  raw message will be displayed.
	 */
	public void displayChatMessage(String message);
	
	/**
	 *  Returns true if this player is in creative mode.
	 */
	public boolean isCreative();
	
	/**
	 *  Returns true if this player is sneaking.
	 */
	public boolean isSneaking();
	
	/**
	 *  Gets the currently-leashed entity for this player, or null if it doesn't exist.
	 */
	public IWrapperEntity getLeashedEntity();
	
	/**
	 *  Returns the held item.  Only valid for {@link AItemBase} items.
	 */
	public AItemBase getHeldItem();
	
	/**
	 *  Returns the held stack as a wrapper.
	 */
	public IWrapperItemStack getHeldStack();
	
	/**
	 *  Gets the inventory of the player.
	 */
	public IWrapperInventory getInventory();
	
	/**
	 *  Sends a packet to this player over the network.
	 *  Convenience method so we don't need to call the
	 *  {@link IInterfaceNetwork} for player-specific packets.
	 */
	public void sendPacket(APacketBase packet);
	
	/**
	 *  Opens the crafting table GUI.  This overrides the normal GUI opened
	 *  when a block is clicked, which allows players to open a GUI by clicking
	 *  an entity instead.  Required as normally MC checks if there is a block
	 *  present in the internal code, which automatically closes the GUI.
	 */
	public void openCraftingGUI();
	
	/**
	 *  Opens the GUI for the passed-in TE, or fails to open any GUI if the TE doesn't have one.
	 *  Actual validity of the GUI being open is left to the TE implementation.
	 *  Note: This method is for any TE that has inventory.  This includes, but is not limited to,
	 *  chests, furnaces, and brewing stands.
	 */
	public void openTileEntityGUI(IWrapperTileEntity tile);
}