package minecrafttransportsimulator.mcinterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.guis.instances.GUIConfig;
import minecrafttransportsimulator.jsondefs.JSONConfig.ConfigJoystick;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.ControlSystem;
import minecrafttransportsimulator.systems.ControlSystem.ControlsJoystick;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for MC input classes.  Used to query various input settings
 * for the {@link ControlSystem}.  Note that {@link #initJoysticks()} runs
 * in a thread.  As such do NOT call any joystick methods except {@link #isJoystickSupportEnabled()}
 * until it returns true.  Once it does, joysicks may be used.  If it does not, it means joysick
 * support is not enabled.  Note that for component indexes, axis always come first, then buttons, but
 * indexes are not re-used.  Therefore, the first button index will vary depending on how many axis
 * are present on any given joystick.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceInput{
	//Common variables.
	private static KeyBinding configKey;
	
	//Mouse variables.
	private static boolean enableMouse = false;
	private static InhibitableMouseHelper customMouseHelper = new InhibitableMouseHelper();
	
	//Joystick variables.
	private static boolean runningJoystickThread = false;
	private static boolean runningClassicMode = false;
	private static boolean joystickLoadingAttempted = false;
	private static boolean joystickEnabled = false;
	private static boolean joystickBlocked = false;
	private static boolean joystickInhibited = false;
	private static final Map<String, Integer> joystickNameCounters = new HashMap<String, Integer>();
	
	//Normal mode joystick variables.
	private static final Map<String, org.lwjgl.input.Controller> joystickMap = new LinkedHashMap<String, org.lwjgl.input.Controller>();
	private static final Map<String, Integer> joystickAxisCountMap = new LinkedHashMap<String, Integer>();
	
	//Classic mode joystick variables.
	private static final Map<String, net.java.games.input.Controller> classicJoystickMap = new LinkedHashMap<String, net.java.games.input.Controller>();
	
	/**
	 *  Called to set the master config key.  This is used by MC to allow us to use MC-controls to open
	 *  the config menu.  Done here as players should at least see something in the controls menu to
	 *  cause them to open the config menu for the actual control configuration.
	 */
	public static void initConfigKey(){
		configKey = new KeyBinding("key.mts.config", Keyboard.KEY_P, "key.categories." + MasterLoader.MODID);
		ClientRegistry.registerKeyBinding(configKey);
	}
	
	/**
	 *  Tries to populate all joysticks into the map.  Called automatically on first key-press seen on the keyboard as we can be
	 *  assured the game is running and the configs are loaded by that time.  May be called manually at other times when
	 *  the joysticks mapped needs to be refreshed.
	 */
	public static void initJoysticks(){
		//Populate the joystick device map.
		//Joystick will be enabled if at least one controller is found.  If none are found, we likely have an error.
		//We can re-try this if the user removes their mouse and we re-run this method.
		if(!runningJoystickThread){
			runningJoystickThread = true;
			joystickBlocked = true;
			Thread joystickThread = new Thread(){
				@Override
				public void run(){
					try{
						joystickNameCounters.clear();
						if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceCore.logError("Starting controller init.");
						if(runningClassicMode){
							if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceCore.logError("Running classic mode.");
							classicJoystickMap.clear();
							if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceCore.logError("Found this many controllers: " + net.java.games.input.ControllerEnvironment.getDefaultEnvironment().getControllers().length);
							for(net.java.games.input.Controller joystick : net.java.games.input.ControllerEnvironment.getDefaultEnvironment().getControllers()){
								joystickEnabled = true;
								if(joystick.getType() != null && !joystick.getType().equals(net.java.games.input.Controller.Type.MOUSE) && !joystick.getType().equals(net.java.games.input.Controller.Type.KEYBOARD) && joystick.getName() != null && joystick.getComponents().length != 0){
									String joystickName = joystick.getName();
									if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceCore.logError("Found valid controller: " + joystickName);
									
									//Add an index on this joystick to be sure we don't override multi-component units.
									if(!joystickNameCounters.containsKey(joystickName)){
										joystickNameCounters.put(joystickName, 0);
									}
									classicJoystickMap.put(joystickName + "_" + joystickNameCounters.get(joystickName), joystick);
									joystickNameCounters.put(joystickName, joystickNameCounters.get(joystickName) + 1);
								}
							}
						}else{
							if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceCore.logError("Running modern mode.");
							if(!org.lwjgl.input.Controllers.isCreated()){
								if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceCore.logError("Creating controller object.");
								org.lwjgl.input.Controllers.create();
							}
							joystickMap.clear();
							joystickAxisCountMap.clear();
							if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceCore.logError("Found this many controllers: " + org.lwjgl.input.Controllers.getControllerCount());
							for(int i=0; i<org.lwjgl.input.Controllers.getControllerCount(); ++i){
								joystickEnabled = true;
								org.lwjgl.input.Controller joystick = org.lwjgl.input.Controllers.getController(i);
								if(joystick.getAxisCount() > 0 && joystick.getButtonCount() > 0 && joystick.getName() != null){
									String joystickName = joystick.getName();
									if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceCore.logError("Found valid controller: " + joystickName);
									
									//Add an index on this joystick to be sure we don't override multi-component units.
									if(!joystickNameCounters.containsKey(joystickName)){
										joystickNameCounters.put(joystickName, 0);
									}
									joystickMap.put(joystickName + "_" + joystickNameCounters.get(joystickName), joystick);
									joystickAxisCountMap.put(joystickName + "_" + joystickNameCounters.get(joystickName), joystick.getAxisCount());
									joystickNameCounters.put(joystickName, joystickNameCounters.get(joystickName) + 1);
								}
							}
						}
						
						//Validate joysticks are valid for this setup by making sure indexes aren't out of bounds.
						Iterator<Entry<String, ConfigJoystick>> iterator = ConfigSystem.configObject.controls.joystick.entrySet().iterator();
						if(ConfigSystem.configObject.clientControls.devMode.value)InterfaceCore.logError("Performing button validity checks.");
						while(iterator.hasNext()){
							try{
								Entry<String, ConfigJoystick> controllerEntry = iterator.next();
								ControlsJoystick control = ControlSystem.ControlsJoystick.valueOf(controllerEntry.getKey().toUpperCase());
								ConfigJoystick config = controllerEntry.getValue();
								if(runningClassicMode){
									if(classicJoystickMap.containsKey(config.joystickName)){
										if(classicJoystickMap.get(config.joystickName).getComponents().length <= config.buttonIndex){
											iterator.remove();
										}
									}
								}else{
									if(joystickMap.containsKey(config.joystickName)){
										if(control.isAxis){
											if(joystickMap.get(config.joystickName).getAxisCount() <= config.buttonIndex){
												iterator.remove();
											}
										}else{
											if(joystickMap.get(config.joystickName).getButtonCount() <= config.buttonIndex - joystickAxisCountMap.get(config.joystickName)){
												iterator.remove();
											}
										}
									}
								}
							}catch(Exception e){
								//Invalid control.
								iterator.remove();
							}
						}

						joystickBlocked = false;
					}catch(Exception e){
						e.printStackTrace();
						InterfaceCore.logError(e.getMessage());
						for(StackTraceElement s : e.getStackTrace()){
							InterfaceCore.logError(s.toString());
						}
					}
					runningJoystickThread = false;
				}
			};
			joystickThread.start();
		}
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
	 *  Enables or disables keyboard repeat events.  This should always be set
	 *  when opening a GUI that handles keypresses, but it should be un-set upon closure
	 *  to prevent repeat presses in-game.
	 */
	public static void setKeyboardRepeat(boolean enabled){
		Keyboard.enableRepeatEvents(enabled);
	}
	
	/**
	 *  Returns true if joystick support is enabled (found at least 1 joystick).
	 */
	public static boolean isJoystickSupportEnabled(){
		return joystickEnabled;
	}
	
	/**
	 *  Returns true if joystick support is blocked.  This happens if the joysick support
	 *  wasn't able to be checked, either due to a bad driver or locked-up thread.
	 */
	public static boolean isJoystickSupportBlocked(){
		return joystickBlocked;
	}
	
	/**
	 *  Returns true if the passed-in joystick is present.
	 *  Can be used to check if a joystick is plugged-in before polling, and if it isn't
	 *  fallback logic for keyboard controls can be used.  Note that if joysticks are inhibited,
	 *  this call will always return false, even if the passed-in joystick exists.
	 */
	public static boolean isJoystickPresent(String joystickName){
		return !joystickInhibited && runningClassicMode ? classicJoystickMap.containsKey(joystickName) : joystickMap.containsKey(joystickName);
	}
	
	/**
	 *  Returns a list of all joysticks currently present on the system.
	 */
	public static List<String> getAllJoystickNames(){
		return new ArrayList<String>(runningClassicMode ? classicJoystickMap.keySet() : joystickMap.keySet());
	}
	
	/**
	 *  Returns the number of axis and buttons the passed-in joystick has.
	 *  Axis will always come before buttons.
	 */
	public static int getJoystickComponentCount(String joystickName){
		return runningClassicMode ? classicJoystickMap.get(joystickName).getComponents().length : joystickMap.get(joystickName).getAxisCount() + joystickMap.get(joystickName).getButtonCount();
	}
	
	/**
	 *  Returns the name of the passed-in component.
	 */
	public static String getJoystickComponentName(String joystickName, int index){
		return runningClassicMode ? classicJoystickMap.get(joystickName).getComponents()[index].getName() : (isJoystickComponentAxis(joystickName, index) ? joystickMap.get(joystickName).getAxisName(index) : joystickMap.get(joystickName).getButtonName(index - joystickAxisCountMap.get(joystickName)));
	}
	
	/**
	 *  Returns true if the component at the passed-in index is an axis.
	 */
	public static boolean isJoystickComponentAxis(String joystickName, int index){
		return runningClassicMode ? classicJoystickMap.get(joystickName).getComponents()[index].isAnalog() : joystickMap.get(joystickName).getAxisCount() > index;
	}
	
	/**
	 *  Returns the current value of the joystick axis.
	 */
	public static float getJoystickAxisValue(String joystickName, int index){
		//Check to make sure this control is operational before testing.  It could have been removed from a prior game.
		if(runningClassicMode){
			if(classicJoystickMap.containsKey(joystickName)){
				classicJoystickMap.get(joystickName).poll();
				return classicJoystickMap.get(joystickName).getComponents()[index].getPollData();
			}else{
				return 0;
			}
		}else{
			//Make sure we're not calling this on non-axis.
			if(joystickMap.containsKey(joystickName)){
				if(isJoystickComponentAxis(joystickName, index)){
					joystickMap.get(joystickName).poll();
					return joystickMap.get(joystickName).getAxisValue(index);
				}else{
					return getJoystickButtonValue(joystickName, index) ? 1 : 0;
				}
			}else{
				return 0;
			}
		}
	}
	
	/**
	 *  Returns the current button-state for the joystick axis.  Note that this is used
	 *  for both analog axis, and fake-digital buttons like Xbox D-pads.
	 */
	public static boolean getJoystickButtonValue(String joystickName, int index){
		//Check to make sure this control is operational before testing.  It could have been removed from a prior game.
		if(runningClassicMode){
			if(classicJoystickMap.containsKey(joystickName)){
				classicJoystickMap.get(joystickName).poll();
				return classicJoystickMap.get(joystickName).getComponents()[index].getPollData() > 0;
			}else{
				return false;
			}
		}else{
			if(joystickMap.containsKey(joystickName)){
				joystickMap.get(joystickName).poll();
				return joystickMap.get(joystickName).isButtonPressed(index - joystickAxisCountMap.get(joystickName));
			}else{
				return false;
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
	 *  Returns the latest mouse deltas as a long comprised of two ints.  The
	 *  first half being the X-coord, and the second half being the Y-coord.  
	 *  Note that this method can only get the delta the mouse has moved, not the absolute
	 *  change, so unless you call this every tick you will miss mouse movement!
	 */
	public static long getMouseDelta(){
		return (((long) customMouseHelper.deltaXForced) << Integer.SIZE) | (customMouseHelper.deltaYForced & 0xffffffffL);
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
	 *  Returns true if the left mouse-button is down.
	 */
	public static boolean isLeftMouseButtonDown(){
		return Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown();
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
    	//Check if we switched joystick modes.
    	if(runningClassicMode ^ ConfigSystem.configObject.clientControls.classicJystk.value){
    		runningClassicMode = ConfigSystem.configObject.clientControls.classicJystk.value;
    		joystickLoadingAttempted = false;
    	}
    	
    	//Init joysticks if we haven't already tried or if we switched loaders.
    	if(!joystickLoadingAttempted){
    		initJoysticks();
    		joystickLoadingAttempted = true;
    	}
    	
    	//Check if we pressed the config key.
        if(configKey.isPressed() && !InterfaceClient.isGUIOpen()){
        	new GUIConfig();
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
			//Don't capture high deltas, as this is likely due to the game pausing.
			super.mouseXYChange();
			if(!enableMouse){
				deltaXForced = deltaX;
				if(deltaXForced > 100){
					deltaXForced = 0;
				}
				deltaYForced = deltaY;
				if(deltaYForced > 100){
					deltaYForced = 0;
				}
				deltaX = 0;
				deltaY = 0;
			}
		}
	};
}
