package minecrafttransportsimulator.sound;

import java.nio.FloatBuffer;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;

/**Interface for classes that need to have sounds played via the audio system.
 * Designed to be server-safe and only query the class implementing this interface for information
 * about the sound state rather than having that class feed it all back to a non-existent system.
 *
 * @author don_bruce
 */
public interface ISoundProvider{
	
	/**
   	 *  Called to start all sounds when this provider is loaded.
   	 *  Sounds may be started after this; this method is just for
   	 *  initial sounds that may have already been playing when this
   	 *  provider was un-loaded and need to re-start.  While this method
   	 *  may be called multiple places, it is assured to be called if the
   	 *  OpenAL sound system is reset, so make sure that any sounds the
   	 *  provider should be playing are started in this call, even if
   	 *  those sounds are flagged as already playing.
   	 *  
   	 */
    public void startSounds();
    
    /**
	 *  Called to update the passed-in sound.
	 */
    public void updateProviderSound(SoundInstance sound);
    
    /**
	 *  Return the position of this ISoundProvider as a 3-unit FloatBuffer.
	 */
    public FloatBuffer getProviderPosition();
    
    /**
	 *  Return the velocity of this ISoundProvider as a Point3d vector.
	 */
    public Point3d getProviderVelocity();
    
    /**
	 *  Return the world this sound is in.  Required for world loading/unloading.
	 */
    public IWrapperWorld getProviderWorld();
}
