package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.AEntityC_Definable;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class represents a rotatable object of a model.
 *
 * @author don_bruce
 */
public class TransformRotatable<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	protected final Point3d rotationAxis;
	
	public TransformRotatable(JSONAnimationDefinition definition){
		super(definition);
		//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
		//rotation magnitude is the magnitude of that vector.
		this.rotationAxis = definition.axis.copy().normalize();
	}

	@Override
	public double applyTransform(AnimationEntity entity, float partialTicks, double offset){
		//Get rotation.
		double rotation = entity.getAnimator().getAnimatedVariableValue(entity, definition, offset, getClock(entity), partialTicks);
		
		//Do rotation.
		if(rotation != 0){
			GL11.glTranslated(definition.centerPoint.x, definition.centerPoint.y, definition.centerPoint.z);
			GL11.glRotated(rotation, rotationAxis.x, rotationAxis.y, rotationAxis.z);
			GL11.glTranslated(-definition.centerPoint.x, -definition.centerPoint.y, -definition.centerPoint.z);
		}
		return rotation;
	}
}
