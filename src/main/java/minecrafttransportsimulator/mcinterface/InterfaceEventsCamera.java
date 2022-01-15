package minecrafttransportsimulator.mcinterface;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Orientation3d;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.systems.CameraSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for handling events pertaining to camera orientation.  This class is responsible for handling
 * camera position and orientation operations.  The only camera functions this class does not handle is overlays,
 * as those are 2D rendering of textures, not 3D orientation of the camera itself. 
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsCamera{
	private static final Point3d cameraAdjustedPosition = new Point3d();
	private static final Orientation3d cameraAdjustedOrientation = new Orientation3d(new Point3d(0, 0, 0));
	
    @SubscribeEvent
    public static void on(CameraSetup event){
    	if(event.getEntity() instanceof EntityPlayer){
    		EntityPlayer mcPlayer = (EntityPlayer) event.getEntity();
    		WrapperPlayer player = WrapperPlayer.getWrapperFor(mcPlayer);
    		cameraAdjustedPosition.set(0, 0, 0);
    		cameraAdjustedOrientation.setAngles(cameraAdjustedPosition);//Really just setting to 0, but don't wanna make another Point3d.
    		if(CameraSystem.adjustCamera(player, cameraAdjustedPosition, cameraAdjustedOrientation, (float) event.getRenderPartialTicks())){
    			//Global settings.  Rotate by 180 to get the forwards-facing orientation; MC does everything backwards.
        		GL11.glRotated(180, 0, 1, 0);
				
				//Now apply our actual offsets.
        		if(cameraAdjustedOrientation.rotation != 0){
        			GL11.glRotated(-cameraAdjustedOrientation.rotation, cameraAdjustedOrientation.axis.x, cameraAdjustedOrientation.axis.y, cameraAdjustedOrientation.axis.z);
        		}
    			GL11.glTranslated(-cameraAdjustedPosition.x, -cameraAdjustedPosition.y, -cameraAdjustedPosition.z);
    			event.setPitch(0);
    			event.setYaw(0);
    			event.setRoll(0);
    		}
    	}
    }
}
