package minecrafttransportsimulator.mcinterface;

import minecrafttransportsimulator.controls.ControlSystem;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

/**Wrapper for the player entity class.  This class wraps the player into a more
 * friendly instance that allows for common operations, like checking if the player
 * has an item, checking if they are OP, etc.  Also prevents the need to interact
 * with the class directly, which allows for abstraction in the code.
 *
 * @author don_bruce
 */
@EventBusSubscriber
public class WrapperPlayer extends WrapperEntity{
	
	public final EntityPlayer player;
	
	public WrapperPlayer(EntityPlayer player){
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
		//FIXME this is why guns don't stay with the player's hands.
		//Player head yaw is their actual yaw.  Head is used for the camera.
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
	
	 /**
     * Reduce the chunk-gen distance to 1 when the player is in a vehicle that's above the set height.
     * This prevents excess lag from world-gen of chunks that don't need to be genned.
     * We also make sure the player has an associated gun entity as part of them if they exist in the world.
     */
    @SubscribeEvent
    public static void on(TickEvent.PlayerTickEvent event){
    	//Only do updates at the end of a phase to prevent double-updates.
        if(event.phase.equals(Phase.END)){
    		if(event.side.isServer()){
    			//If we are on the integrated server, and riding a vehicle, reduce render height.
    			if(event.player.getRidingEntity() instanceof BuilderEntity && ((BuilderEntity) event.player.getRidingEntity()).entity instanceof EntityVehicleF_Physics){
            		WorldServer serverWorld = (WorldServer) event.player.world;
            		if(serverWorld.getMinecraftServer().isSinglePlayer()){
        	    		//If default render distance is 0, we must have not set it yet.
            			//Set both it and the current distance to the actual current distance.
            			if(defaultRenderDistance == 0){
        	    			defaultRenderDistance = serverWorld.getMinecraftServer().getPlayerList().getViewDistance();
        	    			currentRenderDistance = defaultRenderDistance;
        				}
        	    		
            			//If the player is above the configured renderReductionHeight, reduce render.
            			//Once the player drops 10 blocks below it, put the render back to the value it was before.
            			//We don't want to set this every tick as it'll confuse the server.
        	    		if(event.player.posY > ConfigSystem.configObject.clientRendering.renderReductionHeight.value && currentRenderDistance != 1){
        	    			currentRenderDistance = 1;
        	    			serverWorld.getPlayerChunkMap().setPlayerViewRadius(1);
        	    		}else if(event.player.posY < ConfigSystem.configObject.clientRendering.renderReductionHeight.value - 10 && currentRenderDistance == 1){
        	    			currentRenderDistance = defaultRenderDistance;
        	    			serverWorld.getPlayerChunkMap().setPlayerViewRadius(defaultRenderDistance);
        	    		}
        	    	}
    			}
            	
    			//If we don't have an associated gun entity, spawn one now.
            	if(!EntityPlayerGun.playerServerGuns.containsKey(event.player.getUniqueID().toString())){
            		WrapperWorld worldWrapper = WrapperWorld.getWrapperFor(event.player.world);
            		WrapperPlayer playerWrapper = worldWrapper.getWrapperFor(event.player);
            		EntityPlayerGun entity = new EntityPlayerGun(worldWrapper, worldWrapper.generateEntity(), playerWrapper, new WrapperNBT());
            		worldWrapper.spawnEntity(entity);
            	}
        	}else{
        		//If we are holding a gun, check for gun controls.
        		if(EntityPlayerGun.playerClientGuns.containsKey(event.player.getUniqueID().toString())){
        			ControlSystem.controlPlayerGun(EntityPlayerGun.playerClientGuns.get(event.player.getUniqueID().toString()));
            	}
        	}
        }
    }
    private static int defaultRenderDistance = 0;
	private static int currentRenderDistance = 0;
}