package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONCollisionBox{
	@JSONRequired
	@JSONDescription("n entry of x, y, and z coordinates that define the center point of where this collision box is relative to the center of the object.")
	public Point3d pos;
	
	@JSONDescription("The width of this collision box, in meters.  Note that since the pos parameter is the center of the box the box will actually extend � the width in the X and Z direction.")
    public float width;
	
	@JSONDescription("Same as width, just for the Y direction.")
    public float height;
	
	@JSONDescription("If true, the collision box will behave like a ground device set to float.  Note that if you make a boat that uses only these boxes, you'll need one for every corner like you would wheels on a car.  Failing to do so will result in your boat doing a Titanic, just without an iceberg.")
    public boolean collidesWithLiquids;
	
	@JSONDescription("How much armor this collision box has.  Values greater than 0 will make this box use armor code to block bullets from passing through it.  Leaving this value out will make all bullets pass through it (no armor).")
    public float armorThickness;
	
	@JSONDescription("If set, clicking this collision box will toggle the state of this variable.  Useful for doors, though can be used for any toggle-able variable, not just custom doors.")
    public String variableName;
	
	@Deprecated
	public boolean isInterior;
}
