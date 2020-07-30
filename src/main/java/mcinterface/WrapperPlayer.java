package mcinterface;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;

/**Wrapper for the player entity class.  This class wraps the player into a more
 * friendly instance that allows for common operations, like checking if the player
 * has an item, checking if they are OP, etc.  Also prevents the need to interact
 * with the class directly, which allows for abstraction in the code.
 *
 * @author don_bruce
 */
public class WrapperPlayer extends WrapperEntity{
	final EntityPlayer player;
	
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
		Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(BuilderGUI.translate(message)));
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
		//TODO this needs to get removed when we add wrapper itemstacks.
		return itemClass.isInstance(player.getHeldItemMainhand().getItem());
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
	 *  Sets the held stack.
	 */
	public void setHeldStack(ItemStack stack){
		player.setHeldItem(EnumHand.MAIN_HAND, stack);
	}
	
	/**
	 *  Gets the currently-leashed entity for this player, or null if it doesn't exist.
	 */
	public WrapperEntity getLeashedEntity(){
		for(EntityLiving entityLiving : player.world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(player.posX - 7.0D, player.posY - 7.0D, player.posZ - 7.0D, player.posX + 7.0D, player.posY + 7.0D, player.posZ + 7.0D))){
			if(entityLiving.getLeashed() && player.equals(entityLiving.getLeashHolder())){
				entityLiving.clearLeashed(true, !player.capabilities.isCreativeMode);
				return new WrapperEntity(entityLiving);
			}
		}
		return null;
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
	 *  Returns true if this player has all the materials to make the pack-based item.
	 */
	public boolean hasMaterials(AItemPack<? extends AJSONItem<?>> item){
		if(!isCreative()){
			for(ItemStack materialStack : MTSRegistry.getMaterials(item)){
				int requiredMaterialCount = materialStack.getCount();
				for(ItemStack stack : player.inventory.mainInventory){
					if(ItemStack.areItemsEqual(stack, materialStack)){
						requiredMaterialCount -= stack.getCount();
					}
				}
				if(requiredMaterialCount > 0){
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 *  Has the player craft the passed-in item.  Materials are removed if
	 *  required, and the item is added to the player's inventory.
	 *  {@link #hasMaterials(AItemPack)} MUST be called before this method to ensure
	 *  the player actually has the required materials.  Failure to do so will
	 *  result in the player being able to craft the item even if they don't have
	 *  all the materials to do so.
	 */
	public void craftItem(AItemPack<? extends AJSONItem<?>> item){
		for(ItemStack materialStack : MTSRegistry.getMaterials(item)){
			removeItem(materialStack);
		}
		addItem(new ItemStack(item));
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
	 *  Gets the inventory of the player.
	 */
	public IInventory getInventory(){
		//TODO this gets removed, along with the item methods, when we go to wrapper ItemStacks.
		return player.inventory;
	}
	
	/**
	 *  Sends a packet to this player over the network.
	 *  Convenience method so we don't need to call the
	 *  {@link InterfaceNetwork} for player-specific packets.
	 */
	public void sendPacket(APacketBase packet){
		InterfaceNetwork.sendToPlayer(packet, (EntityPlayerMP) player);
	}
}