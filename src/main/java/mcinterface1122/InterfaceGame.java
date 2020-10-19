package mcinterface1122;

import minecrafttransportsimulator.mcinterface.IInterfaceGame;
import minecrafttransportsimulator.mcinterface.IWrapperEntity;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(Side.CLIENT)
class InterfaceGame implements IInterfaceGame{

	@Override
	public boolean isGamePaused(){
		return Minecraft.getMinecraft().isGamePaused();
	}
	
	@Override
	public boolean isChatOpen(){
		return Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen();
	} 
	
	@Override
	public boolean inFirstPerson(){
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
	}
	
	@Override
	public void toggleFirstPerson(){
		if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0){
			Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
		}else{
			Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
		}
	}
	
	@Override
	public IWrapperWorld getClientWorld(){
		return WrapperWorld.getWrapperFor(Minecraft.getMinecraft().world);
	}
	
	@Override
	public IWrapperPlayer getClientPlayer(){
		EntityPlayer player = Minecraft.getMinecraft().player;
		return player != null ? WrapperWorld.getWrapperFor(player.world).getWrapperFor(player) : null;
	}
	
	@Override
	public IWrapperEntity getRenderViewEntity(){
		Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
		return WrapperWorld.getWrapperFor(entity.world).getWrapperFor(entity);
	}
	
    /**
     * Reduce the chunk-gen distance to 1 when the player is in a vehicle that's above the set height.
     * This prevents excess lag from world-gen of chunks that don't need to be genned.
     */
    @SubscribeEvent
    public static void on(TickEvent.PlayerTickEvent event){
    	//Only do updates at the end of a phase to prevent double-updates.
        if(event.phase.equals(Phase.END)){
    		//If we are on the integrated server, and riding a vehicle, reduce render height.
    		if(event.side.isServer()){
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
        	    		if(event.player.posY > ConfigSystem.configObject.client.renderReductionHeight.value && currentRenderDistance != 1){
        	    			currentRenderDistance = 1;
        	    			serverWorld.getPlayerChunkMap().setPlayerViewRadius(1);
        	    		}else if(event.player.posY < ConfigSystem.configObject.client.renderReductionHeight.value - 10 && currentRenderDistance == 1){
        	    			currentRenderDistance = defaultRenderDistance;
        	    			serverWorld.getPlayerChunkMap().setPlayerViewRadius(defaultRenderDistance);
        	    		}
        	    	}
    			}
        	}
        }
    }
    private static int defaultRenderDistance = 0;
	private static int currentRenderDistance = 0;
}
