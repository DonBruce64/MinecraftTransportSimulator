package minecrafttransportsimulator.mcinterface;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**Wrapper for the player entity class.  This class wraps the player into a more
 * friendly instance that allows for common operations, like checking if the player
 * has an item, checking if they are OP, etc.  Also prevents the need to interact
 * with the class directly, which allows for abstraction in the code.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class WrapperPlayer extends WrapperEntity{
	private static final Map<EntityPlayer, WrapperPlayer> playerWrappers = new HashMap<EntityPlayer, WrapperPlayer>();
	
	public final EntityPlayer player;
	
	public WrapperPlayer(EntityPlayer player){
		super(player);
		this.player = player;
	}
	
	/**
	 *  Returns a wrapper instance for the passed-in player instance.
	 *  Null may be passed-in safely to ease function-forwarding.
	 *  Note that the wrapped player class MAY be side-specific, so avoid casting
	 *  the wrapped entity directly if you aren't sure what its class is.
	 *  Wrapper is cached to avoid re-creating the wrapper each time it is requested.
	 */
	public static WrapperPlayer getWrapperFor(EntityPlayer player){
		if(player != null){
			WrapperPlayer wrapper = playerWrappers.get(player);
			if(wrapper == null || !wrapper.isValid() || player != wrapper.player){
				wrapper = new WrapperPlayer(player);
				playerWrappers.put(player, wrapper);
			}
			return wrapper;
		}else{
			return null;
		}
	}
	
	@Override
	public double getSeatOffset(){
		//Player legs are 12 pixels.
		return -12D/16D;
	}

	/**
	 *  Returns true if this player is OP.  Will always return true on single-player worlds.
	 */
	public boolean isOP(){
		return player.getServer() == null || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
	}
	
	/**
	 *  Displays the passed-in chat message to the player.  This interface assumes that the message is
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
		return player.isCreative();
	}
	
	/**
	 *  Returns true if this player is in spectator mode.
	 */
	public boolean isSpectator(){
		return player.isSpectator();
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
				return getWrapperFor(entityLiving);
			}
		}
		return null;
	}
	
	/**
	 *  Returns the held item.  Only valid for {@link AItemBase} items.
	 */
	public AItemBase getHeldItem(){
		Item heldItem = player.getHeldItemMainhand().getItem();
		return heldItem instanceof BuilderItem ? ((BuilderItem) heldItem).item : null;
	}
	
	/**
	 *  Returns the held stack.
	 */
	public ItemStack getHeldStack(){
		return player.getHeldItemMainhand();
	}
	
	/**
	 *  Gets the index of the held stack in the hotbar.
	 */
	public int getHotbarIndex(){
		return player.inventory.currentItem;
	}
	
	/**
	 *  Gets the inventory of the player.
	 */
	public WrapperInventory getInventory(){
		return new WrapperInventory(player.inventory);
	}
	
	/**
	 *  Sends a packet to this player over the network.
	 *  Convenience method so we don't need to call the
	 *  {@link InterfacePacket} for player-specific packets.
	 *  Note that this may ONLY be called on the server, as
	 *  clients don't know about other player's network pipelines.
	 */
	public void sendPacket(APacketBase packet){
		InterfacePacket.sendToPlayer(packet, this);
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
			public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerAccessing){
	            return new ContainerWorkbench(playerInventory, playerAccessing.world, playerAccessing.getPosition()){
	            	@Override
	                public boolean canInteractWith(EntityPlayer playerIn){
	            		return true;
	                }
	            };
	        }
		});
	}
	
	/**
     * Remove all entities from our maps if we unload the world.  This will cause duplicates if we don't.
     */
    @SubscribeEvent
    public static void on(WorldEvent.Unload event){
    	Iterator<EntityPlayer> iterator = playerWrappers.keySet().iterator();
    	while(iterator.hasNext()){
    		if(iterator.next().world.equals(event.getWorld())){
    			iterator.remove();
    		}
    	}
    }
}