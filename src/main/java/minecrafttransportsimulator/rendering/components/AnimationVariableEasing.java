package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;

public class AnimationVariableEasing {

	public double getEasingValue(JSONAnimationDefinition animation, long timeMoved, double value) {
		if (animation.easing != null) {
			switch(animation.easing) {
				case EASEINQUAD: {
					return easeInQuad(animation, timeMoved, value);
				}
				
				case EASEOUTQUAD: {
					return easeOutQuad(animation, timeMoved, value);
				}
				
				case EASEINOUTQUAD: {
					return easeInOutQuad(animation, timeMoved, value);
				}
				
				default: {
					return timeMoved/(double)(animation.duration*50);
				}
			}
			
		} else {
			
			return timeMoved/(double)(animation.duration*50);
			
		}
	}
	
	public double easeInQuad(JSONAnimationDefinition animation, long timeElapsed, double endValue) {
		long duration = animation.duration*50;
		double time = timeElapsed/(double)duration;
		double value = endValue * time * time + 0;
		
		System.out.format("Variable: %s | Value: %f | EndValue: %f | ", animation.variable, value, endValue);
		System.out.format("Duration: " + duration + " | " + "Time Elapsed: " + time + "\n");
		
		return value;
	}
	
	public double easeOutQuad(JSONAnimationDefinition animation, long timeElapsed, double endValue) {
		long duration = animation.duration*50;
		double time = timeElapsed/(double)duration;
		double value = -endValue * time * (time-2) + 0;
		
		return value;
	}
	
	public double easeInOutQuad(JSONAnimationDefinition animation, long timeElapsed, double endValue) {
		long duration = animation.duration*50;
		double time = timeElapsed/(double)duration;
		
		if (time < 1) {
			double value = endValue/2 * time * time + 0;
			return value;
		}
		
		time--;
		
		double value = -endValue/2 * (time * (time - 2) - 1) + 0;
		return value;
	}

}
