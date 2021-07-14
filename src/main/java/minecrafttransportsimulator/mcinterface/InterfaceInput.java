package minecrafttransportsimulator.mcinterface;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIConfig;
import minecrafttransportsimulator.systems.ControlSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for MC input classes.  Used to query various input settings
 * for the {@link ControlSystem}.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceInput{
	//Common variables.
	private static KeyBinding configKey;
	
	//Mouse variables.
	private static boolean enableMouse = false;
	private static int mousePosX = 0;
	private static int mousePosY = 0;
	private static InhibitableMouseHelper customMouseHelper = new InhibitableMouseHelper();
	
	//Joystick variables.
	private static boolean joystickLoadingAttempted = false;
	private static boolean joystickEnabled = false;
	private static boolean joystickInhibited = false;
	private static final Map<String, Controller> joystickMap = new HashMap<String, Controller>();
	private static final Set<Controller> rumblingControllers = new HashSet<Controller>();
	private static final Map<String, Integer> joystickNameCounters = new HashMap<String, Integer>();
	
	/**
	 *  Tries to populate all joysticks into the map.  Called automatically on first key-press seen on the keyboard as we can be
	 *  assured the game is running and the configs are loaded by that time.  May be called manually at other times when
	 *  the joysticks mapped needs to be refreshed.
	 */
	public static void initJoysticks(){
		//Populate the joystick device map.
		//Joystick will be enabled if at least one controller is found.  If none are found, we likely have an error.
		//We can re-try this if the user removes their mouse and we re-run this method.
		try{
			if(!Controllers.isCreated()){
				Controllers.create();
			}
			for(int i=0; i<Controllers.getControllerCount(); ++i){
				Controller joystick = Controllers.getController(i);
				joystickEnabled = true;
				joystickNameCounters.clear();
				if(joystick.getAxisCount() > 0 && joystick.getButtonCount() > 0 && joystick.getName() != null){
					String joystickName = joystick.getName();
					//Add an index on this joystick to be sure we don't override multi-component units.
					if(!joystickNameCounters.containsKey(joystickName)){
						joystickNameCounters.put(joystickName, 0);
					}
					joystickMap.put(joystickName + "_" + joystickNameCounters.get(joystickName), joystick);
					joystickNameCounters.put(joystickName, joystickNameCounters.get(joystickName) + 1);
				}
			}
		}catch(Exception e){}
	}
	
	/**
	 *  Returns the human-readable name for a given integer keycode as defined by the current
	 *  input class.  Integer value and key name may change between versions!  
	 */
	public static String getNameForKeyCode(int keyCode){
		return Keyboard.getKeyName(keyCode);
	}
	
	/**
	 *  Returns the integer keycode for a given human-readable name.
	 *  Integer value and key name may change between versions!
	 */
	public static int getKeyCodeForName(String name){
		return Keyboard.getKeyIndex(name);
	}
	
	/**
	 *  Returns true if the given key is currently pressed.
	 */
	public static boolean isKeyPressed(int keyCode){
		return Keyboard.isKeyDown(keyCode);
	}
	
	/**
	 *  Returns true if joystick support is enabled (found at least 1 joystick).
	 */
	public static boolean isJoystickSupportEnabled(){
		return joystickEnabled;
	}
	
	/**
	 *  Returns true if the passed-in joystick is present.
	 *  Can be used to check if a joystick is plugged-in before polling, and if it isn't
	 *  fallback logic for keyboard controls can be used.  Note that if joysticks are inhibited,
	 *  this call will always return false, even if the passed-in joystick exists.
	 */
	public static boolean isJoystickPresent(String joystickName){
		return !joystickInhibited && joystickMap.containsKey(joystickName);
	}
	
	/**
	 *  Returns a list of all joysticks currently present on the system.
	 */
	public static Set<String> getAllJoysticks(){
		return joystickMap.keySet();
	}
	
	/**
	 *  Returns the number of axis the passed-in joystick has.
	 */
	public static int getJoystickAxisCount(String joystickName){
		return joystickMap.get(joystickName).getAxisCount();
	}
	
	/**
	 *  Returns the number of buttons the passed-in joystick has.
	 */
	public static int getJoystickButtonCount(String joystickName){
		return joystickMap.get(joystickName).getButtonCount();
	}
	
	/**
	 *  Returns the name of the passed-in axis.
	 */
	public static String getJoystickAxisName(String joystickName, int axisIndex){
		return joystickMap.get(joystickName).getAxisName(axisIndex);
	}
	
	/**
	 *  Returns the name of the passed-in axis.
	 */
	public static String getJoystickButtonName(String joystickName, int buttonIndex){
		return joystickMap.get(joystickName).getButtonName(buttonIndex);
	}
	
	/**
	 *  Returns the current value of the joystick axis.
	 */
	public static float getJoystickAxisValue(String joystickName, int axisIndex){
		//Check to make sure this control is operational before testing.  It could have been removed from a prior game.
		if(joystickMap.containsKey(joystickName)){
			joystickMap.get(joystickName).poll();
			return joystickMap.get(joystickName).getAxisValue(axisIndex);
		}else{
			return 0;
		}
	}
	
	/**
	 *  Returns the current button-state for the joystick axis.
	 */
	public static boolean getJoystickButtonValue(String joystickName, int buttonIndex){
		//Check to make sure this control is operational before testing.  It could have been removed from a prior game.
		if(joystickMap.containsKey(joystickName)){
			joystickMap.get(joystickName).poll();
			return joystickMap.get(joystickName).isButtonPressed(buttonIndex);
		}else{
			return false;
		}
	}
	
	/**
	 *  Sets rumble level.  Passed-in value should be between 0-1.
	 */
	public static void setJoystickRumble(String joystickName, float strength){
		Controller joystick = joystickMap.get(joystickName);
		if(rumblingControllers.contains(joystick) ^ strength > 0){
			for(int i=0; i<joystick.getRumblerCount(); ++i){
				joystick.setRumblerStrength(i, strength);
			}
		}
	}
	
	/**
	 *  Inhibits or enables the joysticks.  Inhibiting the joysticks
	 *  will cause {@link #isJoystickPresent(String)} to always return false.
	 *  Useful for when you don't want the joystick to interfere with controls.
	 */
	public static void inhibitJoysticks(boolean inhibited){
		joystickInhibited = inhibited;
	}
	
	/**
	 *  Sets the mouse to be enabled or disabled.  Disabling the mouse
	 *  prevents MC from getting mouse updates, though it does not prevent
	 *  updates from {@link #getTrackedMousePosition()}.
	 */
	public static void setMouseEnabled(boolean enabled){
		enableMouse = enabled;
		//Replace the default MC MouseHelper class with our own.
		//This allows us to disable mouse movement.
		Minecraft.getMinecraft().mouseHelper = customMouseHelper;
	}
	
	/**
	 *  Returns the current mouse deltas as a long comprised of two ints.  The
	 *  first half being the X-coord, and the second half being the Y-coord.  Note
	 *  that this method can only get the delta the mouse has moved, not the absolute
	 *  change, so unless you call this every tick you will get bad data!
	 */
	public static long getTrackedMouseInfo(){
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
	
	/**
	 *  Returns the current  mouse scroll wheel position, if one exists.
	 *  Note that this method can only get the delta the mouse wheel, not the absolute
	 *  change, so unless you call this every tick you will get bad data!
	 */
	public static int getTrackedMouseWheel(){
		return Mouse.hasWheel() ? Mouse.getDWheel() : 0;
	}
	
	/**
	 *  Returns true if the right mouse-button is down.
	 */
	public static boolean isRightMouseButtonDown(){
		return Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown();
	}
	
	/**
     * Opens the config screen when the config key is pressed.
     * Also init the joystick system if we haven't already.
     */
    @SubscribeEvent
    public static void on(InputEvent.KeyInputEvent event){
    	//Init joysticks if we haven't already tried.
    	if(!joystickLoadingAttempted){
    		initJoysticks();
    		joystickLoadingAttempted = true;
    	}
    	
    	//Set the master config key if we don't have it yet.
    	if(configKey == null){
			configKey = new KeyBinding("key.mts.config", Keyboard.KEY_P, "key.categories." + MasterLoader.MODID);
			ClientRegistry.registerKeyBinding(configKey);
    	}
    	
    	//Check if we pressed the config key.
        if(configKey.isPressed() && InterfaceGUI.getActiveGUI() == null){
        	InterfaceGUI.openGUI(new GUIConfig());
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
