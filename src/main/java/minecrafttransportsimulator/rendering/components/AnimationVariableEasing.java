package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;


/**Class designed for calculating the easing equations and returning the interpolated values
 * This is used for interpolating animation values with non-linear equations
 *
 * @author TurboDefender
 */
public class AnimationVariableEasing {

	public double getEasingValue(JSONAnimationDefinition animation, long timeMoved, boolean isReverse) {
		if (animation.forwardsEasing != null && !isReverse) {
			switch(animation.forwardsEasing) {
				case EASEINQUAD: {
					return easeInQuad(animation, timeMoved, false);
				}
				
				case EASEOUTQUAD: {
					return easeOutQuad(animation, timeMoved, false);
				}
				
				case EASEINOUTQUAD: {
					return easeInOutQuad(animation, timeMoved, false);
				}
				
				default: {
					return (timeMoved/(double)(animation.duration*50));
				}
			}
			
		} else if (isReverse) {
			if (animation.reverseEasing != null) {
				switch(animation.reverseEasing) {
					case EASEINQUAD: {
						return easeOutQuad(animation, timeMoved, true);
					}
					
					case EASEOUTQUAD: {
						return easeInQuad(animation, timeMoved, true);
					}
					
					case EASEINOUTQUAD: {
						return easeInOutQuad(animation, timeMoved, true);
					}
					
					default: {
						return (timeMoved/(double)(animation.duration*50));
					}
				}
			} else if (animation.forwardsEasing != null) {
				switch(animation.forwardsEasing) {
					case EASEINQUAD: {
						return easeOutQuad(animation, timeMoved, true);
					}
					
					case EASEOUTQUAD: {
						return easeInQuad(animation, timeMoved, true);
					}
					
					case EASEINOUTQUAD: {
						return easeInOutQuad(animation, timeMoved, true);
					}
					
					default: {
						return (timeMoved/(double)(animation.duration*50));
					}
				}
			} else {
				return (timeMoved/(double)(animation.duration*50));
			}
			
		} else {
			
			return (timeMoved/(double)(animation.duration*50));
			
		}
	}
	
	public double easeInQuad(JSONAnimationDefinition animation, long timeElapsed, boolean isReverse) {
		long duration = animation.duration*50;
		double time = timeElapsed/(double)duration;
		double value;
		if (isReverse) {
			time = 1-time;
		}
		
		value = 1 * time * time + 0;

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
		
		value = -1 * time * (time-2) + 0;
		
		if(isReverse) {
			System.out.format("Direction: Reverse | Value: %f\n", 1-value);
			return 1-value;
		} else {
			System.out.format("Direction: Forwards | Value: %f\n", value);
			return value;
		}
	}
	
	public double easeInOutQuad(JSONAnimationDefinition animation, long timeElapsed, boolean isReverse) {
		long duration = animation.duration*50;
		double time = timeElapsed/((double)duration/2);
		
		if (time < 1) {
			
			double value = 1/(double)2 * time * time + 0;
			
			return value;
			
		} else {
			
			time--;
			
			double value = -1/(double)2 * (time * (time - 2) - 1) + 0;

			return value;
		}
	}

}
