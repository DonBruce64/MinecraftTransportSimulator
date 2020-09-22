package mcinterface;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
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
	
	protected WrapperPlayer(EntityPlayer player){
		super(player);
		this.player = player;
	}
	
	@Override
	public double getSeatOffset(){
		//Player legs are 12 pixels.
		return -12D/16D;
	}
	
	@Override
	public float getHeadYaw(){
		return getYaw();
	}
	
	@Override
	public void setHeadYaw(double yaw){
		setYaw(yaw);
	}
	
	/**
	 *  Returns the player's global UUID.  This is an ID that's unique to every player on Minecraft.
	 *  Useful for assigning ownership where the entity ID of a player might change between sessions.
	 *  <br><br>
	 *  NOTE: While this ID isn't supposed to change, some systems WILL, in fact, change it.  Cracked
	 *  servers, and the nastiest of Bukkit systems will deliberately change the UUID of players, which,
	 *  when combined with their changing of entity IDs, makes server-client lookup impossible.
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
		Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(InterfaceCore.translate(message)));
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
	 *  Gets the currently-leashed entity for this player, or null if it doesn't exist.
	 */
	public WrapperEntity getLeashedEntity(){
		for(EntityLiving entityLiving : player.world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(player.posX - 7.0D, player.posY - 7.0D, player.posZ - 7.0D, player.posX + 7.0D, player.posY + 7.0D, player.posZ + 7.0D))){
			if(entityLiving.getLeashed() && player.equals(entityLiving.getLeashHolder())){
				entityLiving.clearLeashed(true, !player.capabilities.isCreativeMode);
				return WrapperWorld.getWrapperFor(entityLiving.world).getWrapperFor(entityLiving);
			}
		}
		return null;
	}
	
	/**
	 *  Returns the held item.  Only valid for {@link AItemBase} items.
	 */
	public AItemBase getHeldItem(){
		return player.getHeldItemMainhand().getItem() instanceof BuilderItem ? ((BuilderItem) player.getHeldItemMainhand().getItem()).item : null;
	}
	
	/**
	 *  Returns the held stack as a wrapper.
	 */
	public WrapperItemStack getHeldStack(){
		return new WrapperItemStack(player.getHeldItemMainhand());
	}
	
	/**
	 *  Sets the held stack.
	 */
	public void setHeldStack(ItemStack stack){
		player.setHeldItem(EnumHand.MAIN_HAND, stack);
	}
	
	/**
	 *  Returns true if the player has the passed-in item in their inventory.
	 *  This method does not care about NBT, so use the stack method if you need that.
	 */
	public boolean hasItem(AItemBase itemToFind){
		for(ItemStack stack : player.inventory.mainInventory){
			if(BuilderItem.itemWrapperMap.get(itemToFind).equals(stack.getItem())){
				return true;
			}
		}
		return false;
	}
	
	/**
	 *  Attempts to add the passed-in stack to the player's inventory.
	 *  Returns true if addition was successful.  If the player is
	 *  in creative, this will return true, even if the stack
	 *  wasn't added.
	 */
	public boolean addItem(AItemBase item, WrapperNBT data){
		ItemStack stack = new ItemStack(BuilderItem.itemWrapperMap.get(item));
		if(data != null){
			stack.setTagCompound(data.tag);
		}
		return getInventory().addStack(new WrapperItemStack(stack), -1) || player.isCreative();
	}
	
	/**
	 *  Attempts to remove the passed-in item from the player's inventory.
	 *  Returns true if removal was successful.  Note that if the player
	 *  is in creative mode, then removal will not actually occur.
	 */
	public boolean removeItem(AItemBase item, WrapperNBT data){
		if(isCreative()){
			return true;
		}else{
			return player.inventory.clearMatchingItems(BuilderItem.itemWrapperMap.get(item), 0, 1, data != null ? data.tag : null) == 1;
		}
	}
	
	/**
	 *  Attempts to remove the passed-in stack with the passed-in quantity from the player's inventory.
	 *  Returns true if removal was successful.  Note that if the player
	 *  is in creative mode, then removal will not actually occur.
	 */
	public boolean removeStack(WrapperItemStack stack, int qty){
		if(isCreative()){
			return true;
		}else{
			return player.inventory.clearMatchingItems(stack.stack.getItem(), stack.stack.getMetadata(), qty, stack.stack.getTagCompound()) == qty;
		}
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
			removeStack(new WrapperItemStack(materialStack), materialStack.getCount());
		}
		addItem(item, null);
	}
	
	/**
	 *  Gets the inventory of the player.
	 */
	public WrapperInventory getInventory(){
		return new WrapperInventory(this);
	}
	
	/**
	 *  Sends a packet to this player over the network.
	 *  Convenience method so we don't need to call the
	 *  {@link InterfaceNetwork} for player-specific packets.
	 */
	public void sendPacket(APacketBase packet){
		InterfaceNetwork.sendToPlayer(packet, (EntityPlayerMP) player);
	}
	
	/**
	 *  Opens the crafting table GUI.  This overrides the normal GUI opened
	 *  when a block is clicked, which allows players to open a GUI by clicking
	 *  an entity instead.  Required as normally MC checks if there is a block
	 *  present in the internal code, which automatically closes the GUI.
	 */
	public void openCraftingGUI(){
		player.displayGui(new BlockWorkbench.InterfaceCraftingTable(player.world, null){
			@Override
			public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player){
	            return new ContainerWorkbench(playerInventory, player.world, player.getPosition()){
	            	@Override
	                public boolean canInteractWith(EntityPlayer playerIn){
	            		return true;
	                }
	            };
	        }
		});
	}
	
	/**
	 *  Opens the GUI for the passed-in TE, or fails to open any GUI if the TE doesn't have one.
	 *  Actual validity of the GUI being open is left to the TE implementation.
	 *  Note: This method is for any TE that has inventory.  This includes, but is not limited to,
	 *  chests, furnaces, and brewing stands.
	 */
	public void openTileEntityGUI(WrapperTileEntity tile){
		if(tile.tile instanceof IInventory){
			player.displayGUIChest((IInventory) tile.tile);
		}
	}
}