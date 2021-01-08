package minecrafttransportsimulator.rendering.components;

import java.util.HashSet;
import java.util.Set;

import minecrafttransportsimulator.sound.ISoundProviderSimple;

/**Interface for classes that need to have animations done on them and their models.
 * Designed to be server-safe and only query the class implementing this interface for information
 * about the animation state rather than having that class feed it all back to a non-existent system.
 *
 * @author don_bruce
 */
public interface IAnimationProvider extends ISoundProviderSimple{
	public static final Set<String> EMPTY_VARIABLE_SET = new HashSet<String>();
    
    /**
	 *  Returns the animation system for this provider.
	 */
    public AAnimationsBase<?> getAnimationSystem();
    
    /**
   	 *  Returns how much power the lights on the provider have.
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
    public default Set<String> getActiveVariables(){
    	return EMPTY_VARIABLE_SET;
    }
}
