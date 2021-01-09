package mcinterface1122;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.IWrapperInventory;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperTileEntity;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.packets.components.NetworkSystem;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

@EventBusSubscriber
public class WrapperPlayer extends WrapperEntity implements IWrapperPlayer{
	
	public final EntityPlayer player;
	
	WrapperPlayer(EntityPlayer player){
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
		//Player head yaw is their actual yaw.  Head is used for the camera.
		return getYaw();
	}
	
	@Override
	public void setHeadYaw(double yaw){
		setYaw(yaw);
	}
	
	@Override
	public String getUUID(){
		return EntityPlayer.getUUID(player.getGameProfile()).toString();
	}

	@Override
	public boolean isOP(){
		return player.getServer() == null || player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null || player.getServer().isSinglePlayer();
	}
	
	@Override
	public void displayChatMessage(String message){
		Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(MasterInterface.coreInterface.translate(message)));
	}
	
	@Override
	public boolean isCreative(){
		return player.capabilities.isCreativeMode;
	}
	
	@Override
	public boolean isSneaking(){
		return player.isSneaking();
	}
	
	@Override
	public WrapperEntity getLeashedEntity(){
		for(EntityLiving entityLiving : player.world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(player.posX - 7.0D, player.posY - 7.0D, player.posZ - 7.0D, player.posX + 7.0D, player.posY + 7.0D, player.posZ + 7.0D))){
			if(entityLiving.getLeashed() && player.equals(entityLiving.getLeashHolder())){
				entityLiving.clearLeashed(true, !player.capabilities.isCreativeMode);
				return WrapperWorld.getWrapperFor(entityLiving.world).getWrapperFor(entityLiving);
			}
		}
		return null;
	}
	
	@Override
	public AItemBase getHeldItem(){
		return player.getHeldItemMainhand().getItem() instanceof BuilderItem ? ((BuilderItem) player.getHeldItemMainhand().getItem()).item : null;
	}
	
	@Override
	public IWrapperItemStack getHeldStack(){
		return new WrapperItemStack(player.getHeldItemMainhand());
	}
	
	@Override
	public int getHotbarIndex(){
		return player.inventory.currentItem;
	}
	
	@Override
	public IWrapperInventory getInventory(){
		return new WrapperInventory(player.inventory);
	}
	
	@Override
	public void sendPacket(APacketBase packet){
		NetworkSystem.sendToPlayer(packet, this);
	}
	
	@Override
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
	
	@Override
	public void openTileEntityGUI(IWrapperTileEntity tile){
		if(((WrapperTileEntity) tile).tile instanceof IInventory){
			player.displayGUIChest((IInventory) ((WrapperTileEntity) tile).tile);
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
            		EntityPlayerGun entity = new EntityPlayerGun(worldWrapper, worldWrapper.generateEntity(), playerWrapper, MasterLoader.coreInterface.createNewTag());
            		worldWrapper.spawnEntity(entity);
            	}
        	}
        }
    }
    private static int defaultRenderDistance = 0;
	private static int currentRenderDistance = 0;
}