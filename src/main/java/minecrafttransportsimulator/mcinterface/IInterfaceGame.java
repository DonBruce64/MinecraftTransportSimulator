package minecrafttransportsimulator.mcinterface;

/**Interface to the MC game instance.  This class has methods used for determining
 * if the game is paused, the chat window status, and a few other things.
 * This interface interfaces with both Forge and MC code, so if it's something 
 * that's core to the game and doesn't need an instance of an object to access, it's likely here.
 * Note that this interface will not be present on servers, so attempting to access it on such
 * will return null.
 *
 * @author don_bruce
 */
public interface IInterfaceGame{
	/**
	 *  Returns true if the game is paused.
	 */
	public boolean isGamePaused();
	
	/**
	 *  Returns true if the chat window is open.
	 */
	public boolean isChatOpen(); 
	
	/**
	 *  Returns true if the game is in first-person mode.
	 */
	public boolean inFirstPerson();
	
	/**
	 *  Returns true if the game is in standard third-person mode.
	 */
	public boolean inThirdPerson();
	
	/**
	 *  Toggle first-person mode.  This is essentially the same operation as the F5 key.
	 */
	public void toggleFirstPerston();
	
	/**
	 *  Returns the world.  Only valid on CLIENTs as on servers
	 *  there are multiple worlds (dimensions) so a global reference
	 *  isn't possible.
	 */
	public IWrapperWorld getClientWorld();
	
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
}
