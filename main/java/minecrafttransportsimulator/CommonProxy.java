package minecrafttransportsimulator;

import java.io.File;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.multipart.parts.APartEngine;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;

/**Contains registration methods used by {@link MTSRegistry} and methods overridden by ClientProxy. 
 * See the latter for more info on overridden methods.
 * 
 * @author don_bruce
 */
public class CommonProxy{
	public void initConfig(File configFile){
		ConfigSystem.initCommon(configFile);
	}
	
	public void initControls(){}
	public void openGUI(Object clicked, EntityPlayer clicker){}
	public void playSound(Vec3d soundPosition, String soundName, float volume, float pitch){}
	public void addVehicleEngineSound(EntityMultipartE_Vehicle vehicle, APartEngine engine){}
}
