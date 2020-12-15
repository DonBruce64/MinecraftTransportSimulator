package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

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
	public boolean shouldRender(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks){
		double value = definition.offset + getClock(vehicle).getFactoredState(vehicle, VehicleAnimations.getVariableValue(definition.variable,  partialTicks, vehicle, optionalPart));
		return value >= definition.clampMin && value <= definition.clampMax;
	}

	@Override
	public double applyTransform(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks, double offset){
		//Do nothing.
		return 0;
	}
}
