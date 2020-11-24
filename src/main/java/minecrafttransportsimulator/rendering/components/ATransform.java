package minecrafttransportsimulator.rendering.components;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleAnimationDefinition;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Class that when extended allows for a common method of applying
 * transforms.  Transforms are a way to modify the rendering routines
 * that are designed to be universal across all systems and routines.
 * Transforms require a definition of what they are, but other than that
 * the implementation is left up to the specific transform.
 *
 * @author don_bruce
 */
public abstract class ATransform{
	public final VehicleAnimationDefinition definition; 
	private final Map<EntityVehicleF_Physics, DurationDelayClock> clocks = new HashMap<EntityVehicleF_Physics, DurationDelayClock>();
	
	public ATransform(VehicleAnimationDefinition definition){
		this.definition = definition;
	}
	
	/**
	 *  This method should return true if this transform should be rendered.
	 *  By default, all transforms are rendered, but this may be overridden should
	 *  the object this transform is on need to be made invisible.
	 */
	public boolean shouldRender(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks){
		return true;
	}
	
	/*
	 *  This method should return true if this transform should stop other transforms
	 *  from being rendered. This allows disabling certain animations if this animation's
	 *  criteria are met.
	 */
	public boolean shouldInhibit(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks) {
		return false;
	}
	
	/*
	 *  This method should return true if this transform should negate a previous inhibitor.
	 */
	public boolean shouldActivate(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks) {
		return false;
	}
	
	/**
	 *  This method applies any transforms this object has prior to rendering.
	 *  Vehicle and part (if this model is on a part) is passed-in for state-based transform logic.
	 *  Passed-in offset may or may not be used depending on the transform.
	 *  Returned offset should be able to be fed to the next transform down the JSON for its use, if required.
	 */
	public abstract double applyTransform(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks, double offset);
	
	/**
	 *  This method allows for post-render logic.  This allows for additional transforms,
	 *  or transform clean-up (say if lighting was modified).
	 */
	public void doPostRenderLogic(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks){};
	
	/**
	 *  Returns the current animation clock for the passed-in vehicle.  Clocks are not shared between
	 *  vehicle to allow each vehicle to have their own running clocks for each animation.
	 */
	public DurationDelayClock getClock(EntityVehicleF_Physics vehicle){
		if(!clocks.containsKey(vehicle)){
			clocks.put(vehicle, new DurationDelayClock(definition));
		}
		return clocks.get(vehicle);
	}
}
