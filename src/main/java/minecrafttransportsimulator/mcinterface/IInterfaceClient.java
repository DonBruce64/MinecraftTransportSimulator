package minecrafttransportsimulator.mcinterface;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.guis.components.AGUIBase;

/**Interface to the MC client instance.  This class has methods used for determining
 * if the game is paused, the chat window status, and a few other things.
 * This interface interfaces with both Forge and MC code, so if it's something 
 * that's core to the client and doesn't need an instance of an object to access, it's likely here.
 * Note that this interface will not be present on servers, so attempting to access it on such
 * will return null.
 *
 * @author don_bruce
 */
public interface IInterfaceClient{
	
	/**
	 *  Returns true if the game is paused.
	 */
	public boolean isGamePaused();
	
	/**
	 *  Returns the current language name.
	 */
	public String getLanguageName();
	
	/**
	 *  Returns true if the chat window is open.
	 */
	public boolean isChatOpen();
	
	/**
	 *  Returns true if a GUI is open.
	 */
	public boolean isGUIOpen();
	
	/**
	 *  Returns true if the game is in first-person mode.
	 */
	public boolean inFirstPerson();
	
	/**
	 *  Returns true if the game is in third-person mode.
	 *  Does not return true for inverted third-person mode.
	 */
	public boolean inThirdPerson();
	
	/**
	 *  Returns true if the camera mode was switched from last render.
	 *  This is here because some mods will change the camera for rendering,
	 *  and we need to know if the state-change is a switch, or an internal one.
	 */
	public boolean changedCameraState();
	
	/**
	 *  Toggles first-person mode.  This is essentially the same operation as the F5 key.
	 */
	public void toggleFirstPerson();
	
	/**
	 *  Returns the screen width and height as a long comprised of two ints.  The
	 *  first half being the width, and the second half being the height.
	 */
	public long getPackedDisplaySize();
	
	/**
	 *  Returns the current FOV for rendering.  Useful if zoom functions are desired without actually moving the camera.
	 */
	public float getFOV();
	
	/**
	 *  Sets the current FOV for rendering.
	 */
	public void setFOV(float setting);
	
	/**
	 *  Returns the entity we are moused over.  This includes Tile Entities.
	 */
	public AEntityB_Existing getMousedOverEntity();
	
	/**
	 *  Closes the currently-opened GUI, returning back to the main game.
	 *  This should only be done on GUIs where {@link AGUIBase#capturesPlayer()} is true.
	 */
	public void closeGUI();
	
	/**
	 *  Sets the GUI as active.  This will result in it handling key-presses.
	 *  This should only be done on GUIs where {@link AGUIBase#capturesPlayer()} is true.
	 */
	public void setActiveGUI(AGUIBase gui);
	
	/**
	 *  Returns the world.  Only valid on CLIENTs as on servers
	 *  there are multiple worlds (dimensions) so a global reference
	 *  isn't possible.
	 */
	public AWrapperWorld getClientWorld();
	
	/**
	 *  Returns the player.  Only valid on CLIENTs as on servers
	 *  there are multiple players.  Note that the player MAY be null if the
	 *  world hasn't been loaded yet.
	 */
	public IWrapperPlayer getClientPlayer();
	
	/**
	 *  Returns the entity that is used to set up the render camera.
	 *  Normally the player, but can (may?) change.
	 */
	public IWrapperEntity getRenderViewEntity();
	
	/**
	 *  Returns the current camera position.
	 *  The returned position may by modified without affecting the entity's actual position.
	 */
	public Point3D getCameraPosition();
	
	/**
	 *  Plays the block breaking sound for the block at the passed-in position.
	 */
	public void playBlockBreakSound(Point3D position);
	
	/**
	 *  Returns the tooltip lines for the passed-in stack.
	 *  This isn't in the stack itself because tooltips are client-only.
	 */
	public List<String> getTooltipLines(IWrapperItemStack stack);
}
