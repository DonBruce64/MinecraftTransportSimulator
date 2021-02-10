package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class contains methods for animations.  These are used to animate anything 
 * that has access to the world, and provides some helper methods for animations.
 * This class should be extended by any class looking to provide animations.  These
 * classes should then override the appropriate methods of this class to extend its
 * functionality in a transparent manner.
 *
 * @author don_bruce
 */
public abstract class AAnimationsBase<AnimationEntity extends AEntityC_Definable<?>>{
	
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
		return animation.absolute ? Math.abs(value) : value;
	}
	
	/**
	 *  Returns the value for the passed-in variable on the passed-in entity, subject to the clamping, and duration/delay
	 *  requested in the animation definition.  The passed-in offset is used to allow for stacking animations, and should
	 *  be 0 if this functionality is not required.  The passed-in clock may be null to prevent duration/delay functionality.
	 */
	public final double getAnimatedVariableValue(AnimationEntity entity, JSONAnimationDefinition animation, double offset, DurationDelayClock clock, float partialTicks){
		double value = getRawVariableValue(entity, animation.variable, partialTicks);
		if(clock == null || (animation.duration == 0 && animation.forwardsDelay == 0 && animation.reverseDelay == 0)){
			return clampAndScale(value, animation, offset);
		}else{
			return clampAndScale(clock.getFactoredState(entity, value), animation, offset);
		} 
	}
	
	/**
	 *  Returns the raw value for the passed-in variable.  If the variable is not present, 0
	 *  should be returned.
	 */
	public abstract double getRawVariableValue(AnimationEntity entity, String variable, float partialTicks);
	
	/**
	 *  Tries to return the raw value for the passed-in variable, assuming it is a base variable.  If the
	 *  variable is not a base variable, then NaN is returned.  This is to allow overriding functions
	 *  of {@link #getRawVariableValue(IAnimationentity, String, float)} to know if they need to return
	 *  this value, or their own values in their implemented functions.
	 */
	protected static double getBaseVariableValue(AEntityC_Definable<?> entity, String variable, float partialTicks){
		switch(variable){
			case("tick"): return entity.world.getTick();
			case("tick_sin"): return Math.sin(Math.toRadians(entity.world.getTick()));
			case("tick_cos"): return Math.cos(Math.toRadians(entity.world.getTick()));
			case("time"): return entity.world.getTime();
			case("rain_strength"): return (int) entity.world.getRainStrength(entity.position);
			case("rain_sin"): {
				int rainStrength = (int) entity.world.getRainStrength(entity.position); 
				return rainStrength > 0 ? Math.sin(rainStrength*Math.toRadians(360*System.currentTimeMillis()/1000))/2D + 0.5: 0;
			}
			case("rain_cos"): {
				int rainStrength = (int) entity.world.getRainStrength(entity.position); 
				return rainStrength > 0 ? Math.cos(rainStrength*Math.toRadians(360*System.currentTimeMillis()/1000))/2D + 0.5 : 0;
			}	
			case("light_sunlight"): return entity.world.getLightBrightness(entity.position, false);
			case("light_total"): return entity.world.getLightBrightness(entity.position, true);
		}
		
		//Check if this is a cycle variable.
		if(variable.startsWith("cycle")){
			int ticksCycle = Integer.valueOf(variable.substring(variable.indexOf('_') + 1, variable.lastIndexOf('_')));
			int startTick = Integer.valueOf(variable.substring(variable.lastIndexOf('_') + 1));
			return entity.world.getTick()%ticksCycle >= startTick ? 1 : 0;
		}
		
		//Check if this is a generic variable.  This contains lights in most cases.
		if(entity.variablesOn.contains(variable)){
			return 1;
		}
		
		//Didn't find a variable.  Return NaN.
		return Double.NaN;
	}
}
