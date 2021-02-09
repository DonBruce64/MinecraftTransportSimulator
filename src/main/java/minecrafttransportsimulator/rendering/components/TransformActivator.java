package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class represents a pseudo-animation that disables other animations.
 * This transform will disable the other animations of this
 * object if the returned value of the variable is within the min and max clamps.
 *
 * @author gyro_hero
 */
public class TransformActivator<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	
	public TransformActivator(JSONAnimationDefinition definition){
		super(definition);
	}
	
	@Override
	public boolean shouldActivate(AnimationEntity entity, float partialTicks){
		double value = definition.offset + entity.getAnimationValue(definition, 0, getClock(entity), partialTicks);
		return value >= definition.clampMin && value <= definition.clampMax;
	}

	@Override
	public double applyTransform(AnimationEntity entity, float partialTicks, double offset){
		//Do nothing.
		return 0;
	}
}
