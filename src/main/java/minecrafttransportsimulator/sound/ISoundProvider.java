package minecrafttransportsimulator.sound;

import java.nio.FloatBuffer;

/**Interface for classes that need to have sounds played via the audio system.
 * Designed to be server-safe and only query the class implementing this interface for information
 * about the sound state rather than having that class feed it all back to a non-existent system.
 *
 * @author don_bruce
 */
public interface ISoundProvider{	
    /**
	 *  Called to update the passed-in sound.
	 */
    public void updateProviderSound(SoundInstance sound);
    
    /**
   	 *  Called to restart a sound if it has stopped.
   	 *  Used when the system re-loads a sound set.
   	 */
    public void restartSound(SoundInstance sound);
    
    /**
	 *  Return the position of this ISoundProvider as a 3-unit FloatBuffer.
	 */
    public FloatBuffer getProviderPosition();
    
    /**
	 *  Return the velocity of this ISoundProvider as a 3-unit FloatBuffer.
	 */
    public FloatBuffer getProviderVelocity();
    
    /**
	 *  Return the dimension this sound is in.  Required for world loading/unloading.
	 */
    public int getProviderDimension();
}
