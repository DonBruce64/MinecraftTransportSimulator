package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**Contains numerous camera functions for view edits.
 * 
 * @author don_bruce
 */
public final class CameraSystem{
	public static boolean lockedView = true;
	public static boolean disableHUD = false;
	public static int hudMode = 2;
	private static int zoomLevel = 4;
	
	private static final String[] zoomNames = { "thirdPersonDistancePrev", "field_78491_C" };
	
	public static void runCustomCamera(float partialTicks){
		try{
			float rectifiedZoomLevel = (zoomLevel - 4*partialTicks)/(1 - partialTicks);
			ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, Float.valueOf(rectifiedZoomLevel), zoomNames);
		}catch (Exception e){
			MTS.MTSLog.fatal("ERROR IN AIRCRAFT ZOOM REFLECTION!");
			throw new RuntimeException(e);
   	    }
	}
	
	public static void changeCameraZoom(boolean zoomOut){
		if(zoomLevel < 20 && zoomOut){
			++zoomLevel;
		}else if(zoomLevel > 4 && !zoomOut){
			--zoomLevel;
		}
	}
	
	public static void changeCameraLock(){
		lockedView = !lockedView;
		Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}
	
	public static void updatePlayerYawAndPitch(EntityPlayer player, EntityMultipartParent master){
		boolean mouseYoke = ConfigSystem.getBooleanConfig("MouseYoke");
		
		player.renderYawOffset += master.rotationYaw - master.prevRotationYaw;
		if((!mouseYoke && lockedView) || (mouseYoke && !lockedView)){
			player.rotationYaw += master.rotationYaw - master.prevRotationYaw;
			if(master.rotationPitch > 90 || master.rotationPitch < -90){
				player.rotationPitch -= master.rotationPitch - master.prevRotationPitch;
			}else{
				player.rotationPitch += master.rotationPitch - master.prevRotationPitch;
			}
			if((master.rotationPitch > 90 || master.rotationPitch < -90) ^ master.prevRotationPitch > 90 || master.prevRotationPitch < -90){
				player.rotationYaw+=180;
			}
		}else if(mouseYoke){
			if(lockedView){
				player.rotationYaw = master.rotationYaw;
				player.rotationPitch = master.rotationPitch;
			}
		}
	}
}
