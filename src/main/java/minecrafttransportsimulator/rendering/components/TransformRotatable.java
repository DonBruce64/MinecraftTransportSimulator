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
	private final Point3d rotationAxis;
	
	public TransformRotatable(JSONAnimationDefinition definition){
		super(definition);
		//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
		//rotation magnitude is the magnitude of that vector.
		this.rotationAxis = definition.axis.copy();
		rotationAxis.normalize();
	}

	@Override
	public double applyTransform(IAnimationProvider provider, float partialTicks, double offset){
		//Update rotation axis, if required.
		if(provider instanceof EntityVehicleF_Physics && updateRotationAxis((EntityVehicleF_Physics) provider, rotationAxis)){
			rotationAxis.normalize();
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
	
	/**
	 *  Manual helper function for updating the rotaitonAxis of a part prior to rendering.
	 *  Required for auto-rotations.  This function should return true if the axis was updated
	 *  to allow the calling method to save the axis state.
	 */
	protected boolean updateRotationAxis(EntityVehicleF_Physics vehicle, Point3d rotationAxis){
		return false;
	}
}
