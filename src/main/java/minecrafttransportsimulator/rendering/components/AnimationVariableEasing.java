package minecrafttransportsimulator.rendering.components;

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
	
	/**
	 * This is used to check the easing type defined in the JSON fields
	 * and call the respective easing function to return a value
	 * 
	 * @param direction The JSON field either {@code forwardsEasing} or {@code reverseEasing}.
	 * 
	 * @param time The time that has elapsed for an animation or the percent complete from 0 to 1.
	 */
	public static double getEasingType(JSONAnimationDefinition.AnimationEasingType direction, double time) {
		switch(direction) {
			case LINEAR: return time;
			case EASEINQUAD: return easeInQuad(time);
			case EASEOUTQUAD: return easeOutQuad(time);
			case EASEINOUTQUAD: return easeInOutQuad(time);
			case EASEINCUBIC: return easeInCubic(time);
			case EASEOUTCUBIC: return easeOutCubic(time);
			case EASEINOUTCUBIC: return easeInOutCubic(time);
			case EASEINQUART: return easeInQuart(time);
			case EASEOUTQUART: return easeOutQuart(time);
			case EASEINOUTQUART: return easeInOutQuart(time);
			case EASEINQUINT: return easeInQuint(time);
			case EASEOUTQUINT: return easeOutQuint(time);
			case EASEINOUTQUINT: return easeInOutQuint(time);
			case EASEINCIRC: return easeInCirc(time);
			case EASEOUTCIRC: return easeOutCirc(time);
			case EASEINOUTCIRC: return easeInOutCirc(time);
			case EASEINBACK: return easeInBack(time);
			case EASEOUTBACK: return easeOutBack(time);
			case EASEINOUTBACK: return easeInOutBack(time);
			case EASEINELASTIC: return easeInElastic(time);
			case EASEOUTELASTIC: return easeOutElastic(time);
			case EASEINOUTELASTIC: return easeInOutElastic(time);
			case EASEINBOUNCE: return easeInBounce(time);
			case EASEOUTBOUNCE: return easeOutBounce(time);
			case EASEINOUTBOUNCE: return easeInOutBounce(time);
			
			//Easing type is invalid. Default to linear.
			default: return time;
		}
	}
	
	/*Below are functions that calculate the graph of each easing curve
	 *It returns a value according to the time elapsed in an animation
	 */
	public 	static double easeInQuad(double time) {
		return time * time;
	}
	
	public static double easeOutQuad(double time) {
		return time * (2 - time);
	}
	
	public static double easeInOutQuad(double time) {
		if (time < 0.5) {
			return 2 * time * time;
		} else {
			return -1 + (4 - 2 * time) * time;
		}
	}
	
	public static double easeInCubic(double time) {
		return time * time * time;
	}
	
	public static double easeOutCubic(double time) {
		return --time * time * time + 1;
	}
	
	public static double easeInOutCubic(double time) {
		if (time < 0.5) {
			return 4 * time * time * time;
		} else {
			return (time - 1) * (2 * time - 2) * (2 * time - 2) + 1;
		}
	}

	public static double easeInQuart(double time) {
		return time * time * time * time;
	}
	
	public static double easeOutQuart(double time) {
		return 1 - (--time) * time * time * time;
	}
	
	public static double easeInOutQuart(double time) {
		if (time < 0.5) {
			return 8 * time * time * time * time;
		} else {
			return 1 - 8 * (--time) * time * time * time;
		}
	}
	
	public static double easeInQuint(double time) {
		return time * time * time * time * time;
	}
	
	public static double easeOutQuint(double time) {
		return 1 + (--time) * time * time * time * time;
	}
	
	public static double easeInOutQuint(double time) {
		if (time < 0.5) {
			return 16 * time * time * time * time * time;
		} else {
			return 1 + 16 * (--time) * time * time * time * time;
		}
	}
	
	public static double easeInExpo(double time) {
		return time == 0 ? 0 : Math.pow(2, 10 * time - 10);
	}
	
	public static double easeOutExpo(double time) {
		return time == 1 ? 1 : 1 - Math.pow(2, -10 * time);
	}
	
	public static double easeInOutExpo(double time) {
		if (time == 0) {
			return 0;
		} else if (time == 1) {
			return 1;
		} else if (time < 0.5) {
			return Math.pow(2, 20 * time - 10) / 2;
		} else {
			return (2 - Math.pow(2, -20 * time + 10)) / 2;
		}
	}

	public static double easeInCirc(double time) {
		return 1 - Math.sqrt(1 - Math.pow(time, 2));
	}
	
	public static double easeOutCirc(double time) {
		return Math.sqrt(1 - Math.pow(time - 1, 2));
	}
	
	public static double easeInOutCirc(double time) {
		if (time < 0.5) {
			return (1 - Math.sqrt(1 - Math.pow(2 * time, 2))) / 2;
		} else {
			return (Math.sqrt(1 - Math.pow(-2 * time + 2, 2)) + 1) / 2;
		}
	}
	
	public static double easeInBack(double time) {
		return c3 * time * time * time - c1 * time * time;
	}
	
	public static double easeOutBack(double time) {
		return 1 + c3 * Math.pow(time - 1, 3) + c1 * Math.pow(time - 1, 2);
	}
	
	public static double easeInOutBack(double time) {
		if (time < 0.5) {
			return (Math.pow(2 * time, 2) * ((c2 + 1) * 2 * time - c2)) / 2;
		} else {
			return (Math.pow(2 * time - 2, 2) * ((c2 + 1) * (time * 2 - 2) + c2) + 2) / 2;
		}
	}

	public static double easeInElastic(double time) {
		if (time == 0) {
			return 0;
		} else if (time == 1) {
			return 1;
		} else {
			return -Math.pow(2, 10 * time - 10) * Math.sin((time * 10 - 10.75) * c4);
		}
	}
	
	public static double easeOutElastic(double time) {
		if (time == 0) {
			return 0;
		} else if (time == 1) {
			return 1;
		} else {
			return Math.pow(2, -10 * time) * Math.sin((time * 10 - 0.75) * c4) + 1;
		}
	}
	
	public static double easeInOutElastic(double time) {
		if (time == 0) {
			return 0;
		} else if (time == 1) {
			return 1;
		} else if (time < 0.5 ){
			return -(Math.pow(2, 20 * time - 10) * Math.sin((20 * time - 11.125) * c5)) /2;
		} else {
			return (Math.pow(2, -20 * time + 10) * Math.sin((20 * time - 11.125) * c5)) / 2 + 1;
		}
	}

	public static double easeOutBounce(double time) {
		if (time < 1 / d1) {
			return n1 * time * time;
		} else if (time < 2 / d1) {
			return n1 * (time -= 1.5 / d1) * time + 0.75;
		} else if (time < 2.5 / d1) {
			return n1 * (time -= 2.25 / d1) * time + 0.9375;
		} else {
			return n1 * (time -= 2.625 / d1) * time + 0.984375;
		}
	}
	
	public static double easeInBounce(double time) {
		return 1 - easeOutBounce(1 - time);
	}
	
	public static double easeInOutBounce(double time) {
		if (time < 0.5) {
			return (1 - easeOutBounce(1 - 2 * time)) / 2;
		} else {
			return (1 + easeInBounce(2 * time - 1)) / 2;
		}
	}
}
