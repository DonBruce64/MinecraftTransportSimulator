package minecrafttransportsimulator;

import java.io.File;

import minecrafttransportsimulator.baseclasses.Point;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.parts.APartEngine;

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
	public void openGUI(Object clicked, MTSPlayerInterface clicker){}
	public void playSound(Point soundPosition, String soundName, float volume, float pitch){}
	public void addVehicleEngineSound(EntityVehicleE_Powered vehicle, APartEngine engine){}
}
