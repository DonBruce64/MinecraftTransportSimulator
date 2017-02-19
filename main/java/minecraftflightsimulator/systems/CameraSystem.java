package minecraftflightsimulator.systems;

import cpw.mods.fml.common.ObfuscationReflectionHelper;
import minecraftflightsimulator.MFS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

/**Contains numerous camera functions for view edits.
 * 
 * @author don_bruce
 */
public final class CameraSystem{
	public static boolean lockedView = true;
	public static boolean disableHUD = false;
	private static boolean zoomActive = false;
	public static int hudMode = 2;
	private static int zoomLevel = 4;
	private static final String[] rollNames = new String[] {"camRoll", "R", "field_78495_O"};
	private static final String[] zoomNames = new String[] {"thirdPersonDistance", "thirdPersonDistanceTemp", "field_78490_B", "field_78491_C"};

	public static void setCameraZoomActive(boolean active){
		zoomActive = active;
		setCameraZoom();
	}
	
	public static void changeCameraZoom(boolean zoomOut){
		if(zoomLevel < 15 && zoomOut){
			++zoomLevel;
		}else if(zoomLevel > 4 && !zoomOut){
			--zoomLevel;
		}
		setCameraZoom();
	}
	
	private static void setCameraZoom(){
		try{
			ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, zoomActive ? zoomLevel : 4, zoomNames);
		}catch (Exception e){
			System.err.println("ERROR IN AIRCRAFT ZOOM REFLECTION!");
			throw new RuntimeException(e);
		}
	}
	
	public static void changeCameraLock(){
		lockedView = !lockedView;
		MFS.proxy.playSound(Minecraft.getMinecraft().thePlayer, "gui.button.press", 1, 1);
	}
	
	//DEL180START
	//Event system is used in 1.8+ for roll.
	public static void changeCameraRoll(float roll){
		try{
			ObfuscationReflectionHelper.setPrivateValue(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, roll, rollNames);
		}catch (Exception e){
			System.err.println("ERROR IN AIRCRAFT ROLL REFLECTION!");
		}
	}
	//DEL180END
}
