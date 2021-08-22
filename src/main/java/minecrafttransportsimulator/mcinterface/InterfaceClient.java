package minecrafttransportsimulator.mcinterface;

import org.lwjgl.openal.AL;

import minecrafttransportsimulator.baseclasses.Point3d;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.relauncher.Side;

/**Interface to the MC client instance.  This class has methods used for determining
 * if the game is paused, the chat window status, and a few other things.
 * This interface interfaces with both Forge and MC code, so if it's something 
 * that's core to the client and doesn't need an instance of an object to access, it's likely here.
 * Note that this interface will not be present on servers, so attempting to access it on such
 * will return null.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceClient{
	private static boolean lastPassFirstPerson;

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
		//Need to check if we are really in third-person or not.
		//Shaders cause a fake third-person render pass.
		boolean inFirstPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
		if(inFirstPerson || lastPassFirstPerson){
			lastPassFirstPerson = inFirstPerson;
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 *  Returns true if the game is in third-person mode.
	 *  Does not return true for inverted third-person mode.
	 */
	public static boolean inThirdPerson(){
		return Minecraft.getMinecraft().gameSettings.thirdPersonView == 1;
	}
	
	/**
	 *  Toggles first-person mode.  This is essentially the same operation as the F5 key.
	 */
	public static void toggleFirstPerson(){
		if(inFirstPerson()){
			Minecraft.getMinecraft().gameSettings.thirdPersonView = 1;
		}else{
			Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
		}
	}
	
	/**
	 *  Returns the current FOV for rendering.  Useful if zoom functions are desired without actually moving the camera.
	 */
	public static float getFOV(){
		return Minecraft.getMinecraft().gameSettings.fovSetting;
	}
	
	/**
	 *  Sets the current FOV for rendering.
	 */
	public static void setFOV(float setting){
		Minecraft.getMinecraft().gameSettings.fovSetting = setting;
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
		return WrapperPlayer.getWrapperFor(player);
	}
	
	/**
	 *  Returns the entity that is used to set up the render camera.
	 *  Normally the player, but can (may?) change.
	 */
	public static WrapperEntity getRenderViewEntity(){
		Entity entity = Minecraft.getMinecraft().getRenderViewEntity();
		return WrapperEntity.getWrapperFor(entity);
	}
	
	/**
	 *  Returns true OpenAL is ready to play sounds.  This may be changed by mods, so
	 *  this method is here rather than a direct call.
	 */
	public static boolean isSoundSystemReady(){
		return AL.isCreated();
	}
	
	/**
	 *  Plays the block breaking sound for the block at the passed-in position.
	 */
	public static void playBlockBreakSound(Point3d position){
		BlockPos pos = new BlockPos(position.x, position.y, position.z);
		if(!Minecraft.getMinecraft().world.isAirBlock(pos)){
			SoundType soundType = Minecraft.getMinecraft().world.getBlockState(pos).getBlock().getSoundType(Minecraft.getMinecraft().world.getBlockState(pos), Minecraft.getMinecraft().player.world, pos, null);
			Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player, pos, soundType.getBreakSound(), SoundCategory.BLOCKS, soundType.getVolume(), soundType.getPitch());
		}
	}
}
