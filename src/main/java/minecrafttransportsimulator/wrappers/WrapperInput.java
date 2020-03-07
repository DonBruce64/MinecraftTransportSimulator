package minecrafttransportsimulator.wrappers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.guis.instances.GUIConfig;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsKeyboard;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.client.registry.ClientRegistry;

/**Wrapper for MC input classes.  Constructor does not exist, as this is simply a
 * collection of static methods that can be called in placed of calling
 * the lwjgl Keyboard classes as these change in newer versions.  This
 * allows the {@link ControlSystem} to be version-independent.  Note that
 * this wrapper MUST have the {@link #init()} method called, as that sets up
 * the master key binding for opening {@link GUIConfig} for changing the rest
 * of the controls and input settings, as well as checks which controllers are
 * plugged in and able to be used.
 *
 * @author don_bruce
 */
public class WrapperInput{
	//Common variables.
	private static KeyBinding configKey;
	
	//Mouse variables.
	private static boolean enableMouse = false;
	private static int mousePosX = 0;
	private static int mousePosY = 0;
	private static InhibitableMouseHelper customMouseHelper = new InhibitableMouseHelper();
	
	//Joystick variables.
	private static boolean joystickEnabled = false;
	private static final Map<String, Controller> joystickMap = new HashMap<String, Controller>();
	private static final Map<String, Integer> joystickNameCounters = new HashMap<String, Integer>();
	
	/**
	 *  This is called to set up the master keybinding after the main MC systems have started,
	 *  as well as get a list of available controllers and populate the default keybinding map
	 *  with the correct keyCodes.
	 */
	public static void init(){
		//Set the master config key.
		configKey = new KeyBinding("key.mts.config", Keyboard.KEY_P, "key.categories." + MTS.MODID);
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
	
	/**
	 *  Returns true if the master config keybinding is pressed.  Actual key that corresponds to this
	 *  binding may change if it is re-bound by the player in the MC keybinding settings.  
	 */
	public static boolean isMasterControlButttonPressed(){
		return configKey.isPressed();
	}
	
	/**
	 *  Returns the human-readable name for a given integer keycode as defined by the current
	 *  input class.  Interger value and key name may change between versions!  
	 */
	public static String getNameForKeyCode(int keyCode){
		return Keyboard.getKeyName(keyCode);
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
	 *  Returns a list of all joysticks currently present on the system.
	 */
	public static Set<String> getAllJoysticks(){
		return joystickMap.keySet();
	}
	
	/**
	 *  Returns the number of inputs the passed-in joystick has.
	 */
	public static int getJoystickInputCount(String joystickName){
		return joystickMap.get(joystickName).getComponents().length;
	}
	
	/**
	 *  Returns the name of the passed-in input.
	 */
	public static String getJoystickInputName(String joystickName, int buttonIndex){
		return joystickMap.get(joystickName).getComponents()[buttonIndex].getName();
	}
	
	/**
	 *  Returns true if the passed-in input is analog.
	 */
	public static boolean isJoystickInputAnalog(String joystickName, int buttonIndex){
		return joystickMap.get(joystickName).getComponents()[buttonIndex].isAnalog();
	}
	
	/**
	 *  Returns true if the given joystick button is currently pressed.
	 */
	public static boolean isJoystickButtonPressed(String joystickName, int buttonIndex){
		joystickMap.get(joystickName).poll();
		return joystickMap.get(joystickName).getComponents()[buttonIndex].getPollData() > 0;
	}
	
	/**
	 *  Returns the current value of the joystick axis.  Note that this is used
	 *  for both analog axis, and fake-digital buttons like Xbox D-pads.
	 */
	public static float getJoystickInputValue(String joystickName, int axisIndex){
		//Check to make sure this control is operational before testing.  It could have been removed from a prior game.
		if(joystickMap.containsKey(joystickName)){
			joystickMap.get(joystickName).poll();
			return joystickMap.get(joystickName).getComponents()[axisIndex].getPollData();
		}else{
			return 0;
		}
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
			mousePosX = (int) Math.max(Math.min(mousePosX + customMouseHelper.deltaXForced, 250), -250);
		}
		if(Math.abs(customMouseHelper.deltaYForced) < 100){
			mousePosY = (int) Math.max(Math.min(mousePosY + customMouseHelper.deltaYForced, 250), -250);
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
	 *  Returns the default keyCode for the passed-in key.
	 *  Should only be used on first launch when no keyCode has yet been assigned.
	 */
	public static int getDefaultKeyCode(ControlsKeyboard kbEnum){
		switch(kbEnum){
			case AIRCRAFT_MOD: return Keyboard.KEY_RSHIFT;
			case AIRCRAFT_CAMLOCK: return Keyboard.KEY_RCONTROL;
			case AIRCRAFT_YAW_R: return Keyboard.KEY_L;
			case AIRCRAFT_YAW_L: return Keyboard.KEY_J;
			case AIRCRAFT_PITCH_U: return Keyboard.KEY_S;
			case AIRCRAFT_PITCH_D: return Keyboard.KEY_W;
			case AIRCRAFT_ROLL_R: return Keyboard.KEY_D;
			case AIRCRAFT_ROLL_L: return Keyboard.KEY_A;
			case AIRCRAFT_THROTTLE_U: return Keyboard.KEY_I;
			case AIRCRAFT_THROTTLE_D: return Keyboard.KEY_K;
			case AIRCRAFT_FLAPS_U: return Keyboard.KEY_Y;
			case AIRCRAFT_FLAPS_D: return Keyboard.KEY_H;
			case AIRCRAFT_BRAKE: return Keyboard.KEY_B;
			case AIRCRAFT_PANEL: return Keyboard.KEY_U;
			case AIRCRAFT_RADIO: return Keyboard.KEY_MINUS;
			case AIRCRAFT_GUN: return Keyboard.KEY_SPACE;
			case AIRCRAFT_ZOOM_I: return Keyboard.KEY_PRIOR;
			case AIRCRAFT_ZOOM_O: return Keyboard.KEY_NEXT;		
				
			case CAR_MOD: return Keyboard.KEY_RSHIFT;
			case CAR_CAMLOCK: return Keyboard.KEY_RCONTROL;
			case CAR_TURN_R: return Keyboard.KEY_D;
			case CAR_TURN_L: return Keyboard.KEY_A;
			case CAR_GAS: return Keyboard.KEY_W;
			case CAR_BRAKE: return Keyboard.KEY_S;
			case CAR_PANEL: return Keyboard.KEY_U;
			case CAR_SHIFT_U: return Keyboard.KEY_R;
			case CAR_SHIFT_D: return Keyboard.KEY_F;
			case CAR_HORN: return Keyboard.KEY_C;
			case CAR_RADIO: return Keyboard.KEY_MINUS;
			case CAR_GUN: return Keyboard.KEY_SPACE;
			case CAR_ZOOM_I: return Keyboard.KEY_PRIOR;
			case CAR_ZOOM_O: return Keyboard.KEY_NEXT;
			
			default: throw new EnumConstantNotPresentException(ControlsKeyboard.class, kbEnum.name());
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
