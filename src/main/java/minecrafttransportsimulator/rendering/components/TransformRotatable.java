package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**This class represents a rotatable object of a model.
 *
 * @author don_bruce
 */
public class TransformRotatable extends ATransform{
	protected final Point3d rotationAxis;
	
	public TransformRotatable(JSONAnimationDefinition definition){
		super(definition);
		//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
		//rotation magnitude is the magnitude of that vector.
		this.rotationAxis = definition.axis.copy().normalize();
	}

	@Override
	public double applyTransform(IAnimationProvider provider, float partialTicks, double offset){
		//Update tread roller rotation axis, if required.
		if(this instanceof TransformTreadRoller && rotationAxis.isZero()){
			((TransformTreadRoller) this).updateRotationAxis((EntityVehicleF_Physics) provider);
		}
		
		//Get rotation.
		double rotation = provider.getAnimationSystem().getAnimatedVariableValue(provider, definition, offset, getClock(provider), partialTicks);
		
		//Do rotation.
		if(rotation != 0){
			GL11.glTranslated(definition.centerPoint.x, definition.centerPoint.y, definition.centerPoint.z);
			GL11.glRotated(rotation, rotationAxis.x, rotationAxis.y, rotationAxis.z);
			GL11.glTranslated(-definition.centerPoint.x, -definition.centerPoint.y, -definition.centerPoint.z);
		}
		return rotation;
	}
}
