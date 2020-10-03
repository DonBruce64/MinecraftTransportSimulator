package mcinterface1122;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import minecrafttransportsimulator.guis.instances.GUIConfig;
import minecrafttransportsimulator.mcinterface.IInterfaceInput;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(Side.CLIENT)
class InterfaceInput implements IInterfaceInput{
	//Common variables.
	private static KeyBinding configKey;
	
	//Mouse variables.
	private static boolean enableMouse = false;
	private static int mousePosX = 0;
	private static int mousePosY = 0;
	private static InhibitableMouseHelper customMouseHelper = new InhibitableMouseHelper();
	
	//Joystick variables.
	private static boolean joystickEnabled = false;
	private static boolean joystickInhibited = false;
	private static final Map<String, Controller> joystickMap = new HashMap<String, Controller>();
	private static final Map<String, Integer> joystickNameCounters = new HashMap<String, Integer>();
	
	/**
	 *  Static initializer to set up the master keybinding after the main MC systems have started.
	 *  Also getsa list of available controllers and populates the default keybinding map with the correct keyCodes.
	 */
	static{
		//Set the master config key.
		configKey = new KeyBinding("key.mts.config", Keyboard.KEY_P, "key.categories." + MasterInterface.MODID);
		ClientRegistry.registerKeyBinding(configKey);
		
		//Populate the joystick device map.
		//Joystick will be enabled if at least one controller is found.  If none are found, we likely have an error.
		for(Controller joystick : ControllerEnvironment.getDefaultEnvironment().getControllers()){
			joystickEnabled = true;
			if(joystick.getType() != null && joystick.getName() != null){
				if(!joystick.getType().equals(Controller.Type.MOUSE) && !joystick.getType().equals(Controller.Type.KEYBOARD) && !joystick.getType().equals(Controller.Type.UNKNOWN)){
					if(joystick.getComponents().length != 0){
						String joystickName = joystick.getName();
						//Add an index on this joystick to be sure we don't override multi-component units.
						if(!joystickNameCounters.containsKey(joystickName)){
							joystickNameCounters.put(joystickName, 0);
						}
						joystickMap.put(joystickName + "_" + joystickNameCounters.get(joystickName), joystick);
						joystickNameCounters.put(joystickName, joystickNameCounters.get(joystickName) + 1);
					}
				}
			}
		}
	}
	
	@Override
	public String getNameForKeyCode(int keyCode){
		return Keyboard.getKeyName(keyCode);
	}
	
	@Override
	public int getKeyCodeForName(String name){
		return Keyboard.getKeyIndex(name);
	}
	
	@Override
	public boolean isKeyPressed(int keyCode){
		return Keyboard.isKeyDown(keyCode);
	}
	
	@Override
	public boolean isJoystickSupportEnabled(){
		return joystickEnabled;
	}
	
	@Override
	public boolean isJoystickPresent(String joystickName){
		return !joystickInhibited && joystickMap.containsKey(joystickName);
	}
	
	@Override
	public Set<String> getAllJoysticks(){
		return joystickMap.keySet();
	}
	
	@Override
	public int getJoystickInputCount(String joystickName){
		return joystickMap.get(joystickName).getComponents().length;
	}
	
	@Override
	public String getJoystickInputName(String joystickName, int buttonIndex){
		return joystickMap.get(joystickName).getComponents()[buttonIndex].getName();
	}
	
	@Override
	public boolean isJoystickInputAnalog(String joystickName, int buttonIndex){
		return joystickMap.get(joystickName).getComponents()[buttonIndex].isAnalog();
	}
	
	@Override
	public boolean isJoystickButtonPressed(String joystickName, int buttonIndex){
		joystickMap.get(joystickName).poll();
		return joystickMap.get(joystickName).getComponents()[buttonIndex].getPollData() > 0;
	}
	
	@Override
	public float getJoystickInputValue(String joystickName, int axisIndex){
		//Check to make sure this control is operational before testing.  It could have been removed from a prior game.
		if(joystickMap.containsKey(joystickName)){
			joystickMap.get(joystickName).poll();
			return joystickMap.get(joystickName).getComponents()[axisIndex].getPollData();
		}else{
			return 0;
		}
	}
	
	@Override
	public void inhibitJoysticks(boolean inhibited){
		joystickInhibited = inhibited;
	}
	
	@Override
	public void setMouseEnabled(boolean enabled){
		enableMouse = enabled;
		//Replace the default MC MouseHelper class with our own.
		//This allows us to disable mouse movement.
		Minecraft.getMinecraft().mouseHelper = customMouseHelper;
	}
	
	@Override
	public long getTrackedMouseInfo(){
		//Don't want to track mouse if we have a high delta.
		//This usually means we paused the game, which will cause pain if we apply
		//the movement after un-pausing.
		if(Math.abs(customMouseHelper.deltaXForced) < 100){
			mousePosX = Math.max(Math.min(mousePosX + customMouseHelper.deltaXForced, 250), -250);
		}
		if(Math.abs(customMouseHelper.deltaYForced) < 100){
			mousePosY = Math.max(Math.min(mousePosY + customMouseHelper.deltaYForced, 250), -250);
		}
		//Take a unit off of the mouse value to make it more snappy.
		if(mousePosX > 0){
			--mousePosX;
		}else if(mousePosX < 0){
			++mousePosX;
		}
		if(mousePosY > 0){
			--mousePosY;
		}else if(mousePosY < 0){
			++mousePosY;
		}
		return (((long) 2*mousePosX) << Integer.SIZE) | (2*mousePosY & 0xffffffffL);
	}
	
	@Override
	public int getTrackedMouseWheel(){
		return Mouse.hasWheel() ? Mouse.getDWheel() : 0;
	}
	
	/**
     * Opens the config screen when the config key is pressed.
     */
    @SubscribeEvent
    public static void on(InputEvent.KeyInputEvent event){
        if(configKey.isPressed() && MasterInterface.guiInterface.isGUIActive(null)){
        	MasterInterface.guiInterface.openGUI(new GUIConfig());
        }
    }
	
	/**
	 *  Custom MouseHelper class that can have movement checks inhibited based on
	 *  settings in this class.  Allows us to prevent player movement.
	 */
	private static class InhibitableMouseHelper extends MouseHelper{
		private int deltaXForced;
		private int deltaYForced;
		
		@Override
		public void mouseXYChange(){
			//If the mouse is disabled, capture the deltas and prevent MC from seeing them.
			super.mouseXYChange();
			if(!enableMouse){
				deltaXForced = deltaX;
				deltaYForced = deltaY;
				deltaX = 0;
				deltaY = 0;
			}
		}
	};
}
