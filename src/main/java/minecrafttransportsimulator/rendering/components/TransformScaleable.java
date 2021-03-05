package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;

/**This class represents a scalable object of a model.
 *
 * @author don_bruce
 */
public class TransformScaleable<AnimationEntity extends AEntityC_Definable<?>> extends ATransform<AnimationEntity>{
	private final Point3d scalingAxis;
	private final Point3d scalingValue = new Point3d();
	
	public TransformScaleable(JSONAnimationDefinition definition){
		super(definition);
		//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
		//scaling magnitude is the magnitude of that vector.
		this.scalingAxis = definition.axis.copy().normalize();
	}

	@Override
	public double applyTransform(AnimationEntity entity, boolean blendingEnabled, float partialTicks, double offset){
		double scaling = entity.getAnimator().getAnimatedVariableValue(entity, definition, offset, getClock(entity), partialTicks);
		scalingValue.setTo(scalingAxis).multiply(scaling);
		if(scalingAxis.x == 0){
			scalingValue.x = 1.0;
		}
		if(scalingAxis.y == 0){
			scalingValue.y = 1.0;
		}
		if(scalingAxis.z == 0){
			scalingValue.z = 1.0;
		}
		
		//Do the actual scaling.
		GL11.glScaled(scalingValue.x, scalingValue.y, scalingValue.z);
		return scaling;
	}
}
