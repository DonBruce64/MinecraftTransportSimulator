package minecrafttransportsimulator.systems;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartB_Existing;
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
		if(zoomLevel < 40 && zoomOut){
			zoomLevel +=2;
		}else if(zoomLevel > 4 && !zoomOut){
			zoomLevel -=2;
		}
	}
	
	public static void changeCameraLock(){
		lockedView = !lockedView;
		Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}
	
	public static void updatePlayerYawAndPitch(EntityPlayer player, EntityMultipartB_Existing multipart){
		boolean mouseYoke = ConfigSystem.getBooleanConfig("MouseYoke");
		if((!mouseYoke && lockedView) || (mouseYoke && !lockedView)){
			player.rotationYaw += multipart.rotationYaw - multipart.prevRotationYaw;
			if(multipart.rotationPitch > 90 || multipart.rotationPitch < -90){
				player.rotationPitch -= multipart.rotationPitch - multipart.prevRotationPitch;
			}else{
				player.rotationPitch += multipart.rotationPitch - multipart.prevRotationPitch;
			}
			if((multipart.rotationPitch > 90 || multipart.rotationPitch < -90) ^ (multipart.prevRotationPitch > 90 || multipart.prevRotationPitch < -90)){
				player.rotationYaw+=180;
			}
		}else if(mouseYoke){
			if(lockedView){
				player.rotationYaw = multipart.rotationYaw;
				player.rotationPitch = multipart.rotationPitch;
			}
		}
	}
}
