package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class represents a pseudo-animation that disables other animations.
 * This transform will disable the other animations of this
 * object if the returned value of the variable is within the min and max clamps.
 *
 * @author gyro_hero
 */
public class TransformActivator extends ATransform{
	
	public TransformActivator(JSONAnimationDefinition definition){
		super(definition);
	}
	
	@Override
	public boolean shouldActivate(IAnimationProvider provider, float partialTicks){
		double value = definition.offset + provider.getAnimationSystem().getAnimatedVariableValue(provider, definition, 0, getClock(provider), partialTicks);
		return value >= definition.clampMin && value <= definition.clampMax;
	}

	@Override
	public double applyTransform(IAnimationProvider provider, float partialTicks, double offset){
		//Do nothing.
		return 0;
	}
}
