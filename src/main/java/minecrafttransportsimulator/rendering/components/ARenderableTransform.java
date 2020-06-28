package minecrafttransportsimulator.rendering.components;

import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Class that when extended allows for {@link RenderableModelObject}s.
 *
 * @author don_bruce
 */
public abstract class ARenderableTransform{
	
	/**
	 *  This method applies any transforms this object has prior to rendering.
	 *  Vehicle and part (if this model is on a part) is passed-in for state-based transform logic.
	 */
	public abstract void applyTransforms(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks);
	
	/**
	 *  This method allows for post-render logic.  This allows for additional transforms,
	 *  or transform clean-up (say if lighting was modified.
	 */
	public void doPostRenderLogic(EntityVehicleF_Physics vehicle, APart optionalPart, float partialTicks){};
}
