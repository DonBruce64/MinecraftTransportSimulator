package minecraftflightsimulator.utilities;

import minecraftflightsimulator.containers.GUIConfig;
import net.minecraftforge.common.config.ConfigCategory;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**Class that handles all client configuration settings.
 * All configurations are all editable through
 * the config gui {@link GUIConfig}
 * 
 * @author don_bruce
 */
@SideOnly(Side.CLIENT)
public class ClientConfig extends CommonConfig{
	private static final String CLIENT_CONFIG = "clientconfig";
	
	public static void init(){
		booleanConfigMap.put("FirstRun", config.get(CLIENT_CONFIG, "FirstRun", true, "Credits screen is displayed if this is the first run.").getBoolean());
		booleanConfigMap.put("ThrottleKills", config.get(CLIENT_CONFIG, "ThrottleKills", false, "Whether or not the throttle can be put to zero and kill the engine.  \nOnly valid for joysticks.").getBoolean());
		booleanConfigMap.put("SeaLevelOffset", config.get(CLIENT_CONFIG, "SeaLevelOffset", false, "Does altimiter read zero at Y=64 instead of Y=0?").getBoolean());
		booleanConfigMap.put("ElectricStart", config.get(CLIENT_CONFIG, "ElectricStart", true, "Is the starter enabled? \nIf not, players must hit propellers to start them").getBoolean());
		integerConfigMap.put("JoystickForceFactor", config.get(CLIENT_CONFIG, "JoystickForceFactor", 15, "Factor by which joystick inputs are multiplied.  \nThis controls how quickly the control surfaces change.", 1, Byte.MAX_VALUE).getInt());
		integerConfigMap.put("ControlSurfaceCooldown", config.get(CLIENT_CONFIG, "ControlSurfaceCooldown", 4, "How long (in ticks) it takes before control surfaces try to return to their natural angle.  \nThis is not used when using a joystick.", 0, Short.MAX_VALUE).getInt());
		doubleConfigMap.put("DeadZone", config.get(CLIENT_CONFIG, "DeadZone", 0.03D, "Dead zone for joystick axis.  \nMFS will always use the greater of this value and the value provided by the computer.").getDouble());
		config.save();
	}
	
	public static byte getSeaLevelOffset(){
		return (byte) (booleanConfigMap.get("SeaLevelOffset") ? 64 : 0);
	}
	
	@Override
	protected ConfigCategory getConfigCategory(){
		return config.getCategory(CLIENT_CONFIG);
	}
}
