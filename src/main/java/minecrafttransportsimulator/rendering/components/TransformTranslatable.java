package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class represents a translatable object of a model.
 *
 * @author don_bruce
 */
public class TransformTranslatable extends ATransform{
	private final Point3d translationAxis;
	
	
	public TransformTranslatable(JSONAnimationDefinition definition){
		super(definition);
		//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
		//translation magnitude is the magnitude of that vector.
		this.translationAxis = definition.axis.copy().normalize();
	}

	@Override
	public double applyTransform(IAnimationProvider provider, float partialTicks, double offset){
		double translation = provider.getAnimationSystem().getAnimatedVariableValue(provider, definition, offset, getClock(provider), partialTicks);
		//Do the actual translation, if we aren't 0.
		if(translation != 0){
			GL11.glTranslated(translation*translationAxis.x, translation*translationAxis.y, translation*translationAxis.z);
		}
		return translation;
	}
}
