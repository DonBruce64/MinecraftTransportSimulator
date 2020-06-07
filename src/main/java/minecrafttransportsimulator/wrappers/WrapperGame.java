package minecrafttransportsimulator.wrappers;

import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Loader;

/**Wrapper for the core MC game.  This class has methods used for determining
 * which mods are loaded, running main init code during boot, and
 * ensuring all other wrappers are running and ready for calls.  This wrapper
 * interfaces with both Forge and MC code, so if it's something that's core to
 * the game and doesn't need an instance of an object to access, it's likely here.
 *
 * @author don_bruce
 */
public class WrapperGame{	
	/**
	 *  Returns true if the mod with the passed-in modID is present.
	 */
	public static boolean isModPresent(String modID){
		return Loader.isModLoaded(modID);
	}
	
	/**
	 *  Returns the text-based name for the passed-in mod.
	 */
	public static String getModName(String modID){
		return Loader.instance().getIndexedModList().get(modID).getName();
	}
	
	/**
	 *  Returns the text-based name for the passed-in fluid.
	 */
	public static String getFluidName(String fluidID){
		return FluidRegistry.getFluid(fluidID) != null ? new FluidStack(FluidRegistry.getFluid(fluidID), 1).getLocalizedName() : "INVALID";
	}
	
	/**
	 *  Returns true if the game is paused.
	 */
	public static boolean isGamePaused(){
		return Minecraft.getMinecraft().isGamePaused();
	}
	
	/**
	 *  Returns true if the game is in first-person mode.
	 */
	public static boolean inFirstPerson(){
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
	}
	
	/**
	 *  Returns true if the player's sound should be dampened.
	 *  Used if we are in an enclosed vehicle and in first-person mode.
	 *  If the sound is streaming, and the vehicle is the provider, it is
	 *  assumed the sound is the vehicle radio, so it should NOT be dampened.
	 */
	public static boolean shouldSoundBeDampened(SoundInstance sound){
		EntityVehicleE_Powered vehicleRiding = getClientPlayer().getVehicleRiding();
		return vehicleRiding != null && !vehicleRiding.definition.general.openTop && inFirstPerson() && (sound.radio == null || !vehicleRiding.equals(sound.provider));
	}
	
	/**
	 *  Returns the world.  Only valid on CLIENTs as on servers
	 *  there are multiple worlds (dimensions) so a global reference
	 *  isn't possible. 
	 */
	public static WrapperWorld getClientWorld(){
		if(cachedClientWorld == null || !cachedClientWorld.world.equals(Minecraft.getMinecraft().world)){
			cachedClientWorld = new WrapperWorld(Minecraft.getMinecraft().world);
		}
		return cachedClientWorld;
	}
	private static WrapperWorld cachedClientWorld;
	
	/**
	 *  Returns the player.  Only valid on CLIENTs as on servers
	 *  there are multiple players.
	 */
	public static WrapperPlayer getClientPlayer(){
		if(cachedClientPlayer == null || !cachedClientPlayer.entity.equals(Minecraft.getMinecraft().player)){
			cachedClientPlayer = new WrapperPlayer(Minecraft.getMinecraft().player);
		}
		return cachedClientPlayer;
	}
	private static WrapperPlayer cachedClientPlayer;
	
	/**
	 *  Returns the entity that is used to set up the render camera.
	 *  Normally the player, but can (may?) change.
	 */
	public static WrapperEntity getRenderViewEntity(){
		if(cachedRenderViewEntity == null || !cachedRenderViewEntity.entity.equals(Minecraft.getMinecraft().getRenderViewEntity())){
			cachedRenderViewEntity = new WrapperEntity(Minecraft.getMinecraft().getRenderViewEntity());
		}
		return cachedRenderViewEntity;
	}
	private static WrapperEntity cachedRenderViewEntity;
}
