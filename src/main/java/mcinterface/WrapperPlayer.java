package mcinterface;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.items.packs.AItemPack;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

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
	
	@Override
	public double getSeatOffset(){
		//Player legs are 12 pixels.
		return -12D/16D;
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
			removeItem(materialStack, materialStack.getCount());
		}
		addItem(new ItemStack(item));
	}
	
	/**
	 *  Attempts to add the passed-in stack to the player's inventory.
	 *  Returns true if addition was successful.  If the player is
	 *  in creative, this will return true, even if the stack
	 *  wasn't added.
	 */
	public boolean addItem(ItemStack stackToAdd){
		return getInventory().addStack(stackToAdd, -1) || player.isCreative();
	}
	
	/**
	 *  Attempts to remove the passed-in stack from the player's inventory.
	 *  Returns true if removal was successful.  Note that if the player
	 *  is in creative mode, then removal will not actually occur.
	 */
	public boolean removeItem(ItemStack stackToRemove, int qty){
		if(isCreative()){
			return true;
		}else{
			return stackToRemove.getCount() == player.inventory.clearMatchingItems(stackToRemove.getItem(), stackToRemove.getMetadata(), qty, stackToRemove.getTagCompound());
		}
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
	 *  chests, furnaces, and brewing stands.  Also of note: if this TE holds liquids, and the player
	 *  is clicking this TE with liquids, then the liquids will be stored or retrieved from this TE.
	 */
	public void openTileEntityGUI(WrapperTileEntity tile){
		if(tile instanceof IInventory){
			player.displayGUIChest((IInventory) tile);
		}else if(tile.tile instanceof IFluidTank){
			IFluidTank tileTank = (IFluidTank) tile;
			ItemStack stack = player.getHeldItemMainhand();
			if(stack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)){
				IFluidHandlerItem itemHandler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
				//If we are empty or sneaking, drain the tile, otherwise fill.
				//We use 1000 here for the test as buckets will only drain that amount.
				FluidStack drainedTestStack = itemHandler.drain(1000, false);
				if(player.isSneaking() || drainedTestStack == null || drainedTestStack.amount == 0){
					if(tileTank.getFluid() != null){
						tileTank.drain(itemHandler.fill(tileTank.getFluid(), true), true);
					}							
				}else{
					if(tileTank.getFluid() != null){
						tileTank.fill(itemHandler.drain(new FluidStack(tileTank.getFluid().getFluid(), tileTank.getCapacity() - tileTank.getFluidAmount()), true), true);
					}else{
						tileTank.fill(itemHandler.drain(tileTank.getCapacity() - tileTank.getFluidAmount(), true), true);
					}
				}
				player.setHeldItem(EnumHand.MAIN_HAND, itemHandler.getContainer());
			}
		}
	}
}