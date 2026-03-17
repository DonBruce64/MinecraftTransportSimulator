package minecrafttransportsimulator.mcinterface;

import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.systems.CameraSystem.CameraMode;

/**
 * Interface to the MC client instance.  This class has methods used for determining
 * if the game is paused, the chat window status, and a few other things.
 * This interface interfaces with both Forge and MC code, so if it's something
 * that's core to the client and doesn't need an instance of an object to access, it's likely here.
 * Note that this interface will not be present on servers, so attempting to access it on such
 * will return null.
 *
 * @author don_bruce
 */
public interface IInterfaceClient {

    /**
     * Returns true if the game is paused.
     */
    boolean isGamePaused();

    /**
     * Returns the current language name.  Note: this may return the default if you
     * call this method before the game has fully booted.
     */
    String getLanguageName();

    /**
     * Returns a list of all possible language codes the game supports.  It is
     * assured that the result from {@link #getLanguageName()} will be in this list.
     */
    List<String> getAllLanguages();

    /**
     * Returns the in-game name for the passed-in fluid.  If the passed-in fluid is
     * invalid (i.e. there's no fluid registered with the name), then "INVALID" is returned.
     */
    String getFluidName(String fluidID, String fluidMod);

    /**
     * Returns all fluid registry and in-game names as a key-value map.
     */
    Map<String, String> getAllFluidNames();

    /**
     * Returns true if the chat window is open.
     */
    boolean isChatOpen();

    /**
     * Returns true if a GUI is open.
     */
    boolean isGUIOpen();

    /**
     * Displays a short-lived overlay message above the hotbar.
     */
    void displayOverlayMessage(String message);

    /**
     * Returns the camera mode.
     */
    CameraMode getCameraMode();

    /**
     * Sets the camera mode.
     */
    void setCameraMode(CameraMode mode);

    /**
     * Returns the camera-zero default zoom distance.  This can vary from 0 to 4 depending on
     * how the interface handles camera transforms.
     */
    int getCameraDefaultZoom();

    /**
     * Returns the screen width and height as a long comprised of two ints.  The
     * first half being the width, and the second half being the height.
     */
    long getPackedDisplaySize();

    /**
     * Returns the current FOV for rendering.  Useful if zoom functions are desired without actually moving the camera.
     */
    float getFOV();

    /**
     * Sets the current FOV for rendering.
     */
    void setFOV(float setting);

    /**
     * Returns the current mouse sensitivity.  Useful in conjunction with zoomed cameras to reduce travel speed.
     */
    float getMouseSensitivity();

    /**
     * Sets the current mouse sensitivity.
     */
    void setMouseSensitivity(float setting);

    /**
     * Closes the currently-opened GUI, returning back to the main game.
     * This should only be done on GUIs where {@link AGUIBase#capturesPlayer()} is true.
     */
    void closeGUI();

    /**
     * Sets the GUI as active.  This will result in it handling key-presses.
     * This should only be done on GUIs where {@link AGUIBase#capturesPlayer()} is true.
     */
    void setActiveGUI(AGUIBase gui);

    /**
     * Returns the world.  Only valid on CLIENTs as on servers
     * there are multiple worlds (dimensions) so a global reference
     * isn't possible.
     */
    AWrapperWorld getClientWorld();

    /**
     * Returns the player.  Only valid on CLIENTs as on servers
     * there are multiple players.  Note that the player MAY be null if the
     * world hasn't been loaded yet.  Also note that tests to check if any player
     * is this client should use the equals method vs the == operator.
     */
    IWrapperPlayer getClientPlayer();

    /**
     * Returns the current camera position.
     * The returned position may by modified without affecting the entity's actual position.
     */
    Point3D getCameraPosition();

    /**
     * Plays the block breaking sound for the block at the passed-in position.
     */
    void playBlockBreakSound(Point3D position);

    /**
     * Returns the tooltip lines for the passed-in stack.
     * This isn't in the stack itself because tooltips are client-only.
     */
    List<String> getTooltipLines(IWrapperItemStack stack);
}
