package minecrafttransportsimulator.rendering.components;

import java.util.Set;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.sound.ISoundProviderSimple;

/**Interface for classes that need to have animations done on them and their models.
 * Designed to be server-safe and only query the class implementing this interface for information
 * about the animation state rather than having that class feed it all back to a non-existent system.
 *
 * @author don_bruce
 */
public interface IAnimationProvider extends ISoundProviderSimple{
    
    /**
	 *  Returns the position of this provider.
	 */
    public Point3d getProviderPosition();
    
    /**
	 *  Returns the world this provider is in.
	 */
    public IWrapperWorld getProviderWorld();
    
    /**
	 *  Returns the animation system for this provider.
	 */
    public AAnimationsBase getAnimationSystem();
    
    /**
   	 *  Returns how much power have on the lights on this provider.
   	 *  1 is full power, 0 is no power.  Note that this does not directly
   	 *  correspond to rendering of the lights due to different light sections
   	 *  rendering differently at different power levels.
   	 */
    public float getLightPower();
    
    /**
   	 *  Returns all active variables that are active on this provider.
   	 *  Note that this does not include state-based variables specific to
   	 *  individual providers.  Rather, this is for common collections
   	 *  of variables, like lights.
   	 */
    public Set<String> getActiveVariables();
}
