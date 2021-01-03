package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class represents a visibility animation of a model.  This transform will disable the rendering of this
 * object if the returned value of the variable is not within the min and max clamps.
 *
 * @author don_bruce
 */
public class TransformVisibile extends ATransform{
	
	public TransformVisibile(JSONAnimationDefinition definition){
		super(definition);
	}
	
	@Override
	public boolean shouldRender(IAnimationProvider provider, float partialTicks){
		double value = definition.offset + provider.getAnimationSystem().getAnimatedVariableValue(provider, definition, 0, getClock(provider), partialTicks);
		return value >= definition.clampMin && value <= definition.clampMax;
	}

	@Override
	public double applyTransform(IAnimationProvider provider, float partialTicks, double offset){
		//Do nothing.
		return 0;
	}
}
