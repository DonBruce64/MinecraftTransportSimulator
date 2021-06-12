package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;


/**Class designed for calculating the easing equations and returning the interpolated values
 * This is used for interpolating animation values with non-linear equations
 *
 * @author TurboDefender
 */
public class AnimationVariableEasing {
	
	private static final double c1 = 1.70518;
	private static final double c2 = c1 * 1.525;
	private static final double c3 = c1 + 1;
	private static final double c4 = (2 * Math.PI) / 3;
	private static final double c5 = (2 * Math.PI) / 4.5;
	private static final double n1 = 7.5625;
	private static final double d1 = 2.75;
	
	/* This is used to return the interpolated animation values
	 * It call the interpolation methods equivalent to the easing type
	 */
	public double getEasingValue(JSONAnimationDefinition animation, long timeMoved, boolean isReverse) {
		
		double time = timeMoved/(double)(animation.duration*50);
		
		//Check if the animation is playing in reverse
		if (isReverse) {
			//Check if reverseEasing is not omitted
			if (animation.reverseEasing != null) {
				switch(animation.reverseEasing) {
					case LINEAR: {
						return time;
					}
					
					case EASEINQUAD: {
						return easeInQuad(time);
					}
					
					case EASEOUTQUAD: {
						return easeOutQuad(time);
					}
					
					case EASEINOUTQUAD: {
						return easeInOutQuad(time);
					}
					
					case EASEINCUBIC: {
						return easeInCubic(time);
					}
					
					case EASEOUTCUBIC: {
						return easeOutCubic(time);
					}
					
					case EASEINOUTCUBIC: {
						return easeInOutCubic(time);
					}
					
					case EASEINQUART: {
						return easeInQuart(time);
					}
					
					case EASEOUTQUART: {
						return easeOutQuart(time);
					}
					
					case EASEINOUTQUART: {
						return easeInOutQuart(time);
					}
					
					case EASEINQUINT: {
						return easeInQuint(time);
					}
					
					case EASEOUTQUINT: {
						return easeOutQuint(time);
					}
					
					case EASEINOUTQUINT: {
						return easeInOutQuint(time);
					}
					
					case EASEINCIRC: {
						return easeInCirc(time);
					}
					
					case EASEOUTCIRC: {
						return easeOutCirc(time);
					}
					
					case EASEINOUTCIRC: {
						return easeInOutCirc(time);
					}
					
					case EASEINBACK: {
						return easeInBack(time);
					}
					
					case EASEOUTBACK: {
						return easeOutBack(time);
					}
					
					case EASEINOUTBACK: {
						return easeInOutBack(time);
					}
					
					case EASEINELASTIC: {
						return easeInElastic(time);
					}
					
					case EASEOUTELASTIC: {
						return easeOutElastic(time);
					}
					
					case EASEINOUTELASTIC: {
						return easeInOutElastic(time);
					}
					
					case EASEINBOUNCE: {
						return easeInBounce(time);
					}
					
					case EASEOUTBOUNCE: {
						return easeOutBounce(time);
					}
					
					case EASEINOUTBOUNCE: {
						return easeInOutBounce(time);
					}
					//Easing type is invalid. Log error
					default: {
						InterfaceCore.logError("Easing type " + animation.forwardsEasing + " is invalid. Defaulting to linear.");
						return time;
					}
				}
			//If it is, then check if forwardsEasing isn't omitted
			//We can use it's value to apply an easing type to the reverse animation
			} else if (animation.forwardsEasing != null) {
				switch(animation.forwardsEasing) {
					case LINEAR: {
						return time;
					}
					
					case EASEINQUAD: {
						return easeInQuad(time);
					}
					
					case EASEOUTQUAD: {
						return easeOutQuad(time);
					}
					
					case EASEINOUTQUAD: {
						return easeInOutQuad(time);
					}
					
					case EASEINCUBIC: {
						return easeInCubic(time);
					}
					
					case EASEOUTCUBIC: {
						return easeOutCubic(time);
					}
					
					case EASEINOUTCUBIC: {
						return easeInOutCubic(time);
					}
					
					case EASEINQUART: {
						return easeInQuart(time);
					}
					
					case EASEOUTQUART: {
						return easeOutQuart(time);
					}
					
					case EASEINOUTQUART: {
						return easeInOutQuart(time);
					}
					
					case EASEINQUINT: {
						return easeInQuint(time);
					}
					
					case EASEOUTQUINT: {
						return easeOutQuint(time);
					}
					
					case EASEINOUTQUINT: {
						return easeInOutQuint(time);
					}
					
					case EASEINCIRC: {
						return easeInCirc(time);
					}
					
					case EASEOUTCIRC: {
						return easeOutCirc(time);
					}
					
					case EASEINOUTCIRC: {
						return easeInOutCirc(time);
					}
					
					case EASEINBACK: {
						return easeInBack(time);
					}
					
					case EASEOUTBACK: {
						return easeOutBack(time);
					}
					
					case EASEINOUTBACK: {
						return easeInOutBack(time);
					}
					
					case EASEINELASTIC: {
						return easeInElastic(time);
					}
					
					case EASEOUTELASTIC: {
						return easeOutElastic(time);
					}
					
					case EASEINOUTELASTIC: {
						return easeInOutElastic(time);
					}
					
					case EASEINBOUNCE: {
						return easeInBounce(time);
					}
					
					case EASEOUTBOUNCE: {
						return easeOutBounce(time);
					}
					
					case EASEINOUTBOUNCE: {
						return easeInOutBounce(time);
					}
					//Easing type is invalid. Log error
					default: {
						InterfaceCore.logError("Easing type " + animation.forwardsEasing + " is invalid. Defaulting to linear.");
						return time;
					}
				}
			//If both are omitted, then apply linear easing
			} else {
				return time;
			}
		//If animation is playing forwards
		} else {
			//Check if forwardsEasing isn't omitted
			if (animation.forwardsEasing != null) {
				switch(animation.forwardsEasing) {
					case LINEAR: {
						return time;
					}
					
					case EASEINQUAD: {
						return easeInQuad(time);
					}
					
					case EASEOUTQUAD: {
						return easeOutQuad(time);
					}
					
					case EASEINOUTQUAD: {
						return easeInOutQuad(time);
					}
					
					case EASEINCUBIC: {
						return easeInCubic(time);
					}
					
					case EASEOUTCUBIC: {
						return easeOutCubic(time);
					}
					
					case EASEINOUTCUBIC: {
						return easeInOutCubic(time);
					}
					
					case EASEINQUART: {
						return easeInQuart(time);
					}
					
					case EASEOUTQUART: {
						return easeOutQuart(time);
					}
					
					case EASEINOUTQUART: {
						return easeInOutQuart(time);
					}
					
					case EASEINQUINT: {
						return easeInQuint(time);
					}
					
					case EASEOUTQUINT: {
						return easeOutQuint(time);
					}
					
					case EASEINOUTQUINT: {
						return easeInOutQuint(time);
					}
					
					case EASEINCIRC: {
						return easeInCirc(time);
					}
					
					case EASEOUTCIRC: {
						return easeOutCirc(time);
					}
					
					case EASEINOUTCIRC: {
						return easeInOutCirc(time);
					}
					
					case EASEINBACK: {
						return easeInBack(time);
					}
					
					case EASEOUTBACK: {
						return easeOutBack(time);
					}
					
					case EASEINOUTBACK: {
						return easeInOutBack(time);
					}
					
					case EASEINELASTIC: {
						return easeInElastic(time);
					}
					
					case EASEOUTELASTIC: {
						return easeOutElastic(time);
					}
					
					case EASEINOUTELASTIC: {
						return easeInOutElastic(time);
					}
					
					case EASEINBOUNCE: {
						return easeInBounce(time);
					}
					
					case EASEOUTBOUNCE: {
						return easeOutBounce(time);
					}
					
					case EASEINOUTBOUNCE: {
						return easeInOutBounce(time);
					}
					//Easing type is invalid. Log error
					default: {
						InterfaceCore.logError("Easing type " + animation.forwardsEasing + " is invalid. Defaulting to linear.");
						return time;
					}
				}
			//If it is, then apply linear easing
			} else {
				return time;
			}
		}
	}
	
	/* Below this are methods that calculate the graph of each easing curve
	 * It returns a value according to the value provided to timeElapsed
	 */

	public double easeInQuad(double time) {
		
		double value = time * time;

		return value;
	}
	
	public double easeOutQuad(double time) {

		double value = time * (2 - time);
		
		return value;
	}
	
	public double easeInOutQuad(double time) {
		
		if (time < 0.5) {
			
			double value = 2 * time * time;
			
			return value;
			
		} else {
			
			double value = -1 + (4 - 2 * time) * time;

			return value;
			
		}
	}
	
	public double easeInCubic(double time) {
		
		double value = time * time * time;
		
		return value;
	}
	
	public double easeOutCubic(double time) {
		
		double value = --time * time * time + 1;
		
		return value;
	}
	
	public double easeInOutCubic(double time) {
		if (time < 0.5) {
			
			double value = 4 * time * time * time;
			
			return value;

		} else {
			
			double value = (time - 1) * (2 * time - 2) * (2 * time - 2) + 1;
			
			return value;
			
		}
		
	}

	public double easeInQuart(double time) {
		
		double value = time * time * time * time;
		
		return value;
	}
	
	public double easeOutQuart(double time) {
		
		double value = 1 - (--time) * time * time * time;
		
		return value;
	}
	
	public double easeInOutQuart(double time) {
		if (time < 0.5) {
			
			double value = 8 * time * time * time * time;
			
			return value;

		} else {
			
			double value = 1 - 8 * (--time) * time * time * time;
			
			return value;
			
		}
	}
	
	public double easeInQuint(double time) {
		
		double value = time * time * time * time * time;
		
		return value;
	}
	
	public double easeOutQuint(double time) {
		double value = 1 + (--time) * time * time * time * time;
		
		return value;
	}
	
	public double easeInOutQuint(double time) {
		
		if (time < 0.5) {
			
			double value = 16 * time * time * time * time * time;
			
			return value;

		} else {
			
			double value = 1 + 16 * (--time) * time * time * time * time;
			
			return value;
			
		}
	}
	
	public double easeInExpo(double time) {
		
		double value = time == 0 ? 0 : Math.pow(2, 10 * time - 10);
		
		return value;
	}
	
	public double easeOutExpo(double time) {
		
		double value = time == 1 ? 1 : 1 - Math.pow(2, -10 * time);
		
		return value;
	}
	
	public double easeInOutExpo(double time) {
		
		if (time == 0) {
			
			return 0;
			
		} else if (time == 1) {
			
			return 1;
			
		} else if (time < 0.5) {
			
			double value = Math.pow(2, 20 * time - 10) / 2;
			
			return value;
			
		} else {
			
			double value = (2 - Math.pow(2, -20 * time + 10)) / 2;
			
			return value;
			
		}
	}

	public double easeInCirc(double time) {
		
		double value = 1 - Math.sqrt(1 - Math.pow(time, 2));
		
		return value;
	}
	
	public double easeOutCirc(double time) {
		
		double value = Math.sqrt(1 - Math.pow(time - 1, 2));
		
		return value;
	}
	
	public double easeInOutCirc(double time) {
		
		if (time < 0.5) {
			
			double value = (1 - Math.sqrt(1 - Math.pow(2 * time, 2))) / 2;
			
			return value;
			
		} else {
			
			double value = (Math.sqrt(1 - Math.pow(-2 * time + 2, 2)) + 1) / 2;
			
			return value;
			
		}
	}
	
	public double easeInBack(double time) {
		
		
		double value = c3 * time * time * time - c1 * time * time;
		
		return value;
	}
	
	public double easeOutBack(double time) {
		
		double value = 1 + c3 * Math.pow(time - 1, 3) + c1 * Math.pow(time - 1, 2);
		
		return value;
	}
	
	public double easeInOutBack(double time) {
		
		if (time < 0.5) {
			
			double value = (Math.pow(2 * time, 2) * ((c2 + 1) * 2 * time - c2)) / 2;
			
			return value;
			
		} else {
			
			double value = (Math.pow(2 * time - 2, 2) * ((c2 + 1) * (time * 2 - 2) + c2) + 2) / 2;
			
			return value;
			
		}
	}

	public double easeInElastic(double time) {
		
		if (time == 0) {
			
			return 0;
			
		} else if (time == 1) {
			
			return 1;
			
		} else {
			
			double value = -Math.pow(2, 10 * time - 10) * Math.sin((time * 10 - 10.75) * c4);
			
			return value;
		}
	}
	
	public double easeOutElastic(double time) {
		
		if (time == 0) {
			
			return 0;
			
		} else if (time == 1) {
			
			return 1;
			
		} else {
			
			double value = Math.pow(2, -10 * time) * Math.sin((time * 10 - 0.75) * c4) + 1;
			
			return value;
		}
	}
	
	public double easeInOutElastic(double time) {
		
		if (time == 0) {
		
			return 0;
		
		} else if (time == 1) {
			
			return 1;
			
		} else if (time < 0.5 ){
			
			double value = -(Math.pow(2, 20 * time - 10) * Math.sin((20 * time - 11.125) * c5)) /2;
			
			return value;
			
		} else {
			
			double value = (Math.pow(2, -20 * time + 10) * Math.sin((20 * time - 11.125) * c5)) / 2 + 1;
			
			return value;
		}
	}

	public double easeOutBounce(double time) {
		
		if (time < 1 / d1) {
			
			double value = n1 * time * time;
			
			return value;
			
		} else if (time < 2 / d1) {
			
		    double value = n1 * (time -= 1.5 / d1) * time + 0.75;
		    
			return value;
			
		} else if (time < 2.5 / d1) {
			
		    double value = n1 * (time -= 2.25 / d1) * time + 0.9375;
		    
			return value;
			
		} else {
			
		    double value = n1 * (time -= 2.625 / d1) * time + 0.984375;
		    
			return value;
		}
	}
	
	public double easeInBounce(double time) {
		
		double value = 1 - easeOutBounce(1 - time);
		
		return value;
	}
	
	public double easeInOutBounce(double time) {
		if (time < 0.5) {
			
			double value = (1 - easeOutBounce(1 - 2 * time)) / 2;
			
			return value;
			
		} else {
			
			double value = (1 + easeInBounce(2 * time - 1)) / 2;
			
			return value;
		}
	}
}
