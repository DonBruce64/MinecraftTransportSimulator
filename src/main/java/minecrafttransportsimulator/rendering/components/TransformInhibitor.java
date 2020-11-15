package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.systems.VehicleAnimationSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**This class represents a pseudo-animation that disables other animations.
 * This transform will disable the other animations of this
 * object if the returned value of the variable is not within the min and max clamps.
 *
 * @author gyro_hero
 */
public class TransformInhibitor extends ATransformRenderable{
	
	public TransformInhibitor(VehicleAnimationDefinition definition){
		super(definition);
	}
	
	@Override
	public boolean shouldInhibit(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks) {
		double value = definition.offset + getClock(vehicle).getFactoredState(vehicle, VehicleAnimationSystem.getVariableValue(definition.variable,  partialTicks, vehicle, optionalPart));
		return value >= definition.clampMin && value <= definition.clampMax;
	}

	@Override
	public double applyTransform(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks, double offset){
		return 0;
	}
}
