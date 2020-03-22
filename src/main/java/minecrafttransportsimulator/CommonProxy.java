package minecrafttransportsimulator;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APartEngine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;

/**Contains registration methods used by {@link MTSRegistry} and methods overridden by ClientProxy. 
 * See the latter for more info on overridden methods.
 * 
 * @author don_bruce
 */
public class CommonProxy{	
	public void initControls(){}
	public void openGUI(Object clicked, EntityPlayer clicker){}
	public void playSound(Vec3d soundPosition, String soundName, float volume, float pitch, EntityVehicleE_Powered optionalVehicle){}
	public void addVehicleEngineSound(EntityVehicleE_Powered vehicle, APartEngine engine){}
}
