package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

public class JSONPhysicsModifier{
	
	@JSONDescription("The name of the physics property to modify.  May be one of the following:")
    public JSONPhysicsProperty property;
	
	@JSONDescription("The value to add to the specified property.")
    public float value;
	
	@JSONDescription("A optional listing of animations used to decide when this modifier is active.  Visibiity animations will completely disable the modifier if they are false.")
	public List<JSONAnimationDefinition> animations;
	
	public enum JSONPhysicsProperty{
		WING_AREA,
		WING_SPAN,
		AILERON_AREA,
		ELEVATOR_AREA,
		RUDDER_AREA,
		DRAG_COEFFICIENT,
		BALLAST_VOLUME,
		DOWN_FORCE,
		BRAKING_FACTOR,
		OVER_STEER,
		UNDER_STEER,
		AXLE_RATIO;
	}
}
