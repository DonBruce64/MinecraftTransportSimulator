package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class represents a visibility animation of a model.  This transform will disable the rendering of this
 * object if the returned value of the variable is not within the min and max clamps.
 *
 * @author don_bruce
 */
public class TransformVisibile<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	
	public TransformVisibile(JSONAnimationDefinition definition){
		super(definition);
	}
	
	@Override
	public boolean shouldRender(AnimationEntity entity, boolean blendingEnabled, float partialTicks){
		double value = definition.offset + entity.getAnimator().getAnimatedVariableValue(entity, definition, 0, getClock(entity), partialTicks);
		return value >= definition.clampMin && value <= definition.clampMax;
	}

	@Override
	public double applyTransform(AnimationEntity provider, boolean blendingEnabled, float partialTicks, double offset){
		//Do nothing.
		return 0;
	}
}
