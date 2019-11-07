package minecrafttransportsimulator.radio;

import net.minecraft.entity.player.EntityPlayer;

/**This interface should be implemented on anything that will have a radio.
 * It lets the {@link Radio} class obtain information about position
 * data it knows how to set its volume and pan levels.  This is put in its own
 * interface separate from the main radio code to allow it to be implemented on
 * classes that exist on the server and not interact with any audio code that
 * doesn't exist there.
 *
 * @author don_bruce
 */
public interface RadioContainer{
	/**Gets the distance between this object and the player.**/
	public abstract double getDistanceToPlayer(EntityPlayer player);
	
	/**Gets the angle between this Container and the player.  Used for stereo panning.**/
	public abstract double getAngleToPlayer();
	
	/**Return true if this container is still valid.  If false, the radio system will stop and delete this
	 * container to keep the audio from playing when this container is removed from the world.**/
	public abstract boolean isValid();
}
