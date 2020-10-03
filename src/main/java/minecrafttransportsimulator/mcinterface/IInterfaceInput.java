package minecrafttransportsimulator.mcinterface;

import java.util.Set;

import minecrafttransportsimulator.systems.ControlSystem;

/**Interface for MC input classes.  Used to query various input settings
 * for the {@link ControlSystem} to allow it to be version-independent.
 * This is because lwjgl Keyboard classes change in newer versions.
 * 
 *
 * @author don_bruce
 */
public interface IInterfaceInput{
	
	/**
	 *  Returns the human-readable name for a given integer keycode as defined by the current
	 *  input class.  Integer value and key name may change between versions!  
	 */
	public String getNameForKeyCode(int keyCode);
	
	/**
	 *  Returns the integer keycode for a given human-readable name.
	 *  Integer value and key name may change between versions!
	 */
	public int getKeyCodeForName(String name);
	
	/**
	 *  Returns true if the given key is currently pressed.
	 */
	public boolean isKeyPressed(int keyCode);
	
	/**
	 *  Returns true if joystick support is enabled (found at least 1 joystick).
	 */
	public boolean isJoystickSupportEnabled();
	
	/**
	 *  Returns true if the passed-in joystick is present.
	 *  Can be used to check if a joystick is plugged-in before polling, and if it isn't
	 *  fallback logic for keyboard controls can be used.  Note that if joysticks are inhibited,
	 *  this call will always return false, even if the passed-in joystick exists.
	 */
	public boolean isJoystickPresent(String joystickName);
	
	/**
	 *  Returns a list of all joysticks currently present on the system.
	 */
	public Set<String> getAllJoysticks();
	
	/**
	 *  Returns the number of inputs the passed-in joystick has.
	 */
	public int getJoystickInputCount(String joystickName);
	
	/**
	 *  Returns the name of the passed-in input.
	 */
	public String getJoystickInputName(String joystickName, int buttonIndex);
	
	/**
	 *  Returns true if the passed-in input is analog.
	 */
	public boolean isJoystickInputAnalog(String joystickName, int buttonIndex);
	
	/**
	 *  Returns true if the given joystick button is currently pressed.
	 */
	public boolean isJoystickButtonPressed(String joystickName, int buttonIndex);
	
	/**
	 *  Returns the current value of the joystick axis.  Note that this is used
	 *  for both analog axis, and fake-digital buttons like Xbox D-pads.
	 */
	public float getJoystickInputValue(String joystickName, int axisIndex);
	
	/**
	 *  Inhibits or enables the joysticks.  Inhibiting the joysticks
	 *  will cause {@link #isJoystickPresent(String)} to always return false.
	 *  Useful for when you don't want the joystick to interfere with controls.
	 */
	public void inhibitJoysticks(boolean inhibited);
	
	/**
	 *  Sets the mouse to be enabled or disabled.  Disabling the mouse
	 *  prevents MC from getting mouse updates, though it does not prevent
	 *  updates from {@link #getTrackedMousePosition()}.
	 */
	public void setMouseEnabled(boolean enabled);
	
	/**
	 *  Returns the current mouse deltas as a long comprised of two ints.  The
	 *  first half being the X-coord, and the second half being the Y-coord.  Note
	 *  that this method can only get the delta the mouse has moved, not the absolute
	 *  change, so unless you call this every tick you will get bad data!
	 */
	public long getTrackedMouseInfo();
	
	/**
	 *  Returns the current  mouse scroll wheel position, if one exists.
	 *  Note that this method can only get the delta the mouse wheel, not the absolute
	 *  change, so unless you call this every tick you will get bad data!
	 */
	public int getTrackedMouseWheel();
}
