package mcinterface;

import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
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

/**Interface for the MC game instance.  This class has methods used for determining
 * if the game is paused, the chat window status, and a few other things.
 * This interface interfaces with both Forge and MC code, so if it's something 
 * that's core to the game and doesn't need an instance of an object to access, it's likely here.
 *
 * @author don_bruce
 */
@Mod.EventBusSubscriber(Side.CLIENT)
public class InterfaceGame{
	/**
	 *  Returns true if the game is paused.
	 */
	public static boolean isGamePaused(){
		return Minecraft.getMinecraft().isGamePaused();
	}
	
	/**
	 *  Returns true if the chat window is open.
	 */
	public static boolean isChatOpen(){
		return Minecraft.getMinecraft().ingameGUI.getChatGUI().getChatOpen();
	} 
	
	/**
	 *  Returns true if the game is in first-person mode.
	 */
	public static boolean inFirstPerson(){
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
	}
	
	/**
	 *  Returns true if the game is in standard third-person mode.
	 */
	public static boolean inThirdPerson(){
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 1;
	}
	
	/**
	 *  Toggle first-person mode.  Does not toggle to the inverted third-person mode.
	 */
	public static void toggleFirstPerston(){
		Minecraft.getMinecraft().gameSettings.thirdPersonView = inFirstPerson() ? 1 : 0;
	}
	
	/**
	 *  Returns true if the player's sound should be dampened.
	 *  Used if we are in an enclosed vehicle and in first-person mode.
	 *  If the sound is streaming, and the vehicle is the provider, it is
	 *  assumed the sound is the vehicle radio, so it should NOT be dampened.
	 */
	public static boolean shouldSoundBeDampened(SoundInstance sound){
		AEntityBase entityRiding = getClientPlayer().getEntityRiding();
		return entityRiding instanceof EntityVehicleF_Physics && !((EntityVehicleF_Physics) entityRiding).definition.general.openTop && inFirstPerson() && (sound.radio == null || !entityRiding.equals(sound.provider));
	}
	
	/**
	 *  Returns the world.  Only valid on CLIENTs as on servers
	 *  there are multiple worlds (dimensions) so a global reference
	 *  isn't possible.
	 */
	public static WrapperWorld getClientWorld(){
		return WrapperWorld.getWrapperFor(Minecraft.getMinecraft().world);
	}
	
	/**
	 *  Returns the player.  Only valid on CLIENTs as on servers
	 *  there are multiple players.  Note that the player MAY be null if the
	 *  world hasn't been loaded yet.
	 */
	public static WrapperPlayer getClientPlayer(){
		EntityPlayer player = Minecraft.getMinecraft().player;
		return player != null ? WrapperWorld.getWrapperFor(player.world).getWrapperFor(player) : null;
	}
	
	/**
	 *  Returns the entity that is used to set up the render camera.
	 *  Normally the player, but can (may?) change.
	 */
	public static WrapperEntity getRenderViewEntity(){
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
