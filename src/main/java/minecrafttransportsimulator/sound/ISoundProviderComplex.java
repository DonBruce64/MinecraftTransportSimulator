package minecrafttransportsimulator.sound;

import minecrafttransportsimulator.baseclasses.Point3d;

/**Interface for classes that need to have complex sounds played via the audio system.
 * These sounds are on a provider that moves, so velocity data is required.
 * This class also assumes that the provider has looping sounds that need to be re-started
 * should it be interrupted.
 *
 * @author don_bruce
 */
public interface ISoundProviderComplex extends ISoundProviderSimple{
	
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
	 *  Return the velocity of this ISoundProvider as a Point3d vector.
	 */
    public Point3d getProviderVelocity();
    
}
