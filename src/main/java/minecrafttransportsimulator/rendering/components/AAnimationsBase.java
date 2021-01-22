package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class contains methods for animations.  These are used to animate anything 
 * that has access to the world, and provides some helper methods for animations.
 * This class should be extended by any class looking to provide animations.  These
 * classes should then override the appropriate methods of this class to extend its
 * functionality in a transparent manner.
 *
 * @author don_bruce
 */
public abstract class AAnimationsBase<ProviderClass extends IAnimationProvider>{
	
	/**
	 *  Clamps and scales the passed-in variable value based on the passed-in animation, returning it in the proper form.
	 */
	protected static double clampAndScale(double value, JSONAnimationDefinition animation, double offset){
		if(animation.axis != null){
			value = animation.axis.length()*(animation.absolute ? Math.abs(value) : value) + animation.offset + offset;
			if(animation.clampMin != 0 && value < animation.clampMin){
				value = animation.clampMin;
			}else if(animation.clampMax != 0 && value > animation.clampMax){
				value = animation.clampMax;
			}
		}
		return value;
	}
	
	/**
	 *  Returns the value for the passed-in variable on the passed-in provider, subject to the clamping, and duration/delay
	 *  requested in the animation definition.  The passed-in offset is used to allow for stacking animations, and should
	 *  be 0 if this functionality is not required.  The passed-in clock may be null to prevent duration/delay functionality.
	 */
	@SuppressWarnings("unchecked")
	public final double getAnimatedVariableValue(IAnimationProvider provider, JSONAnimationDefinition animation, double offset, DurationDelayClock clock, float partialTicks){
		double value = getRawVariableValue((ProviderClass) provider, animation.variable, partialTicks);
		if(clock == null || (animation.duration == 0 && animation.forwardsDelay == 0 && animation.reverseDelay == 0)){
			return clampAndScale(value, animation, offset);
		}else{
			return clampAndScale(clock.getFactoredState(provider, value), animation, offset);
		} 
	}
	
	/**
	 *  Returns the raw value for the passed-in variable.  If the variable is not present, 0
	 *  should be returned.
	 */
	public abstract double getRawVariableValue(ProviderClass provider, String variable, float partialTicks);
	
	/**
	 *  Tries to return the raw value for the passed-in variable, assuming it is a base variable.  If the
	 *  variable is not a base variable, then NaN is returned.  This is to allow overriding functions
	 *  of {@link #getRawVariableValue(IAnimationProvider, String, float)} to know if they need to return
	 *  this value, or their own values in their implemented functions.
	 */
	protected final double getBaseVariableValue(ProviderClass provider, String variable, float partialTicks){
		switch(variable){
			case("tick"): return provider.getProviderWorld().getTick();
			case("tick_sin"): return Math.sin(Math.toRadians(provider.getProviderWorld().getTick()));
			case("tick_cos"): return Math.cos(Math.toRadians(provider.getProviderWorld().getTick()));
			case("time"): return provider.getProviderWorld().getTime();
			case("rain_strength"): return (int) provider.getProviderWorld().getRainStrength(new Point3i(provider.getProviderPosition()));
			case("rain_sin"): {
				int rainStrength = (int) provider.getProviderWorld().getRainStrength(new Point3i(provider.getProviderPosition())); 
				return rainStrength > 0 ? Math.sin(rainStrength*Math.toRadians(360*System.currentTimeMillis()/1000))/2D + 0.5: 0;
			}
			case("rain_cos"): {
				int rainStrength = (int) provider.getProviderWorld().getRainStrength(new Point3i(provider.getProviderPosition())); 
				return rainStrength > 0 ? Math.cos(rainStrength*Math.toRadians(360*System.currentTimeMillis()/1000))/2D + 0.5 : 0;
			}	
			case("light_sunlight"): return provider.getProviderWorld().getLightBrightness(new Point3i(provider.getProviderPosition()), false);
			case("light_total"): return provider.getProviderWorld().getLightBrightness(new Point3i(provider.getProviderPosition()), true);
		}
		
		//Check if this is a cycle variable.
		if(variable.startsWith("cycle")){
			int ticksCycle = Integer.valueOf(variable.substring(variable.indexOf('_') + 1, variable.lastIndexOf('_')));
			int startTick = Integer.valueOf(variable.substring(variable.lastIndexOf('_') + 1));
			return provider.getProviderWorld().getTick()%ticksCycle >= startTick ? 1 : 0;
		}
		
		//Check if this is a generic variable.  This contains lights in most cases.
		if(provider.getActiveVariables().contains(variable)){
			return 1;
		}
		
		//Didn't find a variable.  Return NaN.
		return Double.NaN;
	}
}
