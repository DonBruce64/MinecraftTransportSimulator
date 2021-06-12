package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;


/**Class designed for calculating the easing equations and returning the interpolated values
 * This is used for interpolating animation values with non-linear equations
 *
 * @author TurboDefender
 */
public class AnimationVariableEasing {

	public double getEasingValue(JSONAnimationDefinition animation, long timeMoved, boolean isReverse) {
		
		//Check if the animation is playing in reverse
		if (isReverse) {
			//Check if
			if (animation.reverseEasing != null) {
				switch(animation.reverseEasing) {
					case LINEAR: {
						return linear(animation, timeMoved);
					}
					
					case EASEINQUAD: {
						return easeOutQuad(animation, timeMoved, true);
					}
					
					case EASEOUTQUAD: {
						return easeInQuad(animation, timeMoved, true);
					}
					
					case EASEINOUTQUAD: {
						return easeInOutQuad(animation, timeMoved);
					}
					
					default: {
						InterfaceCore.logError("Easing type " + animation.reverseEasing + " does not exist. Defaulting to linea.");
						return linear(animation, timeMoved);
					}
				}
			} else if (animation.forwardsEasing != null) {
				switch(animation.forwardsEasing) {
					case LINEAR: {
						return linear(animation, timeMoved);
					}
					
					case EASEINQUAD: {
						return easeInQuad(animation, timeMoved, false);
					}
					
					case EASEOUTQUAD: {
						return easeOutQuad(animation, timeMoved, false);
					}
					
					case EASEINOUTQUAD: {
						return easeInOutQuad(animation, timeMoved);
					}
					
					default: {
						InterfaceCore.logError("Easing type " + animation.forwardsEasing + " does not exist. Defaulting to linear.");
						return linear(animation, timeMoved);
					}
				}
			} else {
				return linear(animation, timeMoved);
			}
		} else {
			if (animation.forwardsEasing != null) {
				switch(animation.forwardsEasing) {
					case LINEAR: {
						return linear(animation, timeMoved);
					}
					
					case EASEINQUAD: {
						return easeInQuad(animation, timeMoved, false);
					}
					
					case EASEOUTQUAD: {
						return easeOutQuad(animation, timeMoved, false);
					}
					
					case EASEINOUTQUAD: {
						return easeInOutQuad(animation, timeMoved);
					}
					
					default: {
						InterfaceCore.logError("Easing type " + animation.forwardsEasing + " does not exist. Defaulting to linear.");
						return linear(animation, timeMoved);
					}
				}
			} else {
				return linear(animation, timeMoved);
			}
		}
	}
	
	public double linear(JSONAnimationDefinition animation, long timeElapsed) {
		return (timeElapsed/(double)(animation.duration*50));
	}
	
	/*
	 */
	public double easeInQuad(JSONAnimationDefinition animation, long timeElapsed, boolean isReverse) {
		long duration = animation.duration*50;
		double time = timeElapsed/(double)duration;
		double value;
		if (isReverse) {
			time = 1-time;
		}
		
		value = time * time;

		if(isReverse) {
			System.out.format("Direction: Reverse | Value: %f\n", 1-value);
			return 1-value;
		} else {
			System.out.format("Direction: Forwards | Value: %f\n", value);
			return value;
		}
	}
	
	public double easeOutQuad(JSONAnimationDefinition animation, long timeElapsed, boolean isReverse) {
		long duration = animation.duration*50;
		double time = timeElapsed/(double)duration;
		double value;
		
		if (isReverse) {
			time = 1-time;
		}
		
		value = time * (2-time);
		
		if(isReverse) {
			System.out.format("Direction: Reverse | Value: %f\n", 1-value);
			return 1-value;
		} else {
			System.out.format("Direction: Forwards | Value: %f\n", value);
			return value;
		}
	}
	
	public double easeInOutQuad(JSONAnimationDefinition animation, long timeElapsed) {
		long duration = animation.duration*50;
		double time = timeElapsed/(double)duration;
		
		if (time < .5) {
			
			double value = 2 * time * time;
			
			return value;
			
		} else {
			
			double value = -1 + (4 - 2 * time) * time;

			return value;
		}
	}

}
