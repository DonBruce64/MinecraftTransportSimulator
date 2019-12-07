package minecrafttransportsimulator.systems;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;

/**Contains numerous camera functions for view edits.
 * 
 * @author don_bruce
 */
public final class CameraSystem{
	public static boolean lockedView = true;
	public static boolean disableHUD = false;
	public static int hudMode = 2;
	private static int zoomLevel = 0;
		
	public static void performZoomAction(){
		if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 1){
			GL11.glTranslatef(0, 0F, -zoomLevel);
        }else if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 2){
        	GL11.glTranslatef(0, 0F, zoomLevel);
        }
		//GL11.glRotatef(0, 0, 1, 0);
	}
	
	public static void changeCameraZoom(boolean zoomOut){
		if(zoomOut){
			zoomLevel +=2;
		}else{
			if(zoomLevel > 0){
				zoomLevel -=2;
			}
		}
	}
	
	public static void changeCameraLock(){
		lockedView = !lockedView;
		Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}
	
	public static void updatePlayerYawAndPitch(EntityPlayer player, EntityVehicleB_Existing vehicle){
		boolean mouseYoke = ConfigSystem.getBooleanConfig("MouseYoke");
		if((!mouseYoke && lockedView) || (mouseYoke && !lockedView)){
			player.rotationYaw += vehicle.rotationYaw - vehicle.prevRotationYaw;
			if((vehicle.rotationPitch > 90 || vehicle.rotationPitch < -90) ^ (vehicle.prevRotationPitch > 90 || vehicle.prevRotationPitch < -90)){
				player.rotationYaw+=180;
			}
		}else if(mouseYoke){
			if(lockedView){
				player.rotationYaw = vehicle.rotationYaw;
				player.rotationPitch = vehicle.rotationPitch;
			}
		}
	}
}
