package minecrafttransportsimulator.rendering.components;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.systems.VehicleAnimationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**This class represents a translatable object of a model.
 *
 * @author don_bruce
 */
public class TransformTranslatable extends ATransformRenderable{
	private final Point3d translationAxis;
	private final double translationMagnitude;
	
	
	public TransformTranslatable(VehicleAnimationDefinition definition){
		super(definition);
		//For the axis defined in the JSON, the axis is the normalized value of the defined vector, while the 
		//translation magnitude is the magnitude of that vector.
		this.translationAxis = definition.axis.copy();
		this.translationMagnitude = translationAxis.length();
		translationAxis.normalize();
	}

	@Override
	public double applyTransform(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks, double offset){
		double translation = getClock(vehicle).getFactoredState(vehicle, VehicleAnimationSystem.getVariableValue(definition.variable,  partialTicks, vehicle, null));
		translation = VehicleAnimationSystem.clampAndScale(translation, translationMagnitude, definition.offset + offset, definition.clampMin, definition.clampMax, definition.absolute);
		
		//Do the actual translation, if we aren't 0.
		if(translation != 0){
			GL11.glTranslated(translation*translationAxis.x, translation*translationAxis.y, translation*translationAxis.z);
		}
		return translation;
	}
}
