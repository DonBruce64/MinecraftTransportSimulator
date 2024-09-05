package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.jsondefs.JSONAction.ActionType;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONCollisionBox {
    @JSONRequired
    @JSONDescription("n entry of x, y, and z coordinates that define the center point of where this collision box is relative to the center of the object.")
    public Point3D pos;

    @JSONDescription("The width of this collision box, in meters.  Note that since the pos parameter is the center of the box the box will actually extend � the width in the X and Z direction.")
    public float width;

    @JSONDescription("Same as width, just for the Y direction.")
    public float height;

    @JSONDescription("If true, the collision box will behave like a ground device set to float.  Note that if you make a boat that uses only these boxes, you'll need one for every corner like you would wheels on a car.  Failing to do so will result in your boat doing a Titanic, just without an iceberg.")
    public boolean collidesWithLiquids;

    @JSONDescription("The action to perform when clicking this hitbox, if any.")
    public JSONAction action;

    @Deprecated
    public boolean isInterior;
    @Deprecated
    public float armorThickness;
    @Deprecated
    public float heatArmorThickness;
    @Deprecated
    public float damageMultiplier;
    @Deprecated
    public String variableName;
    @Deprecated
    public ActionType variableType;
    @Deprecated
    public float variableValue;
    @Deprecated
    public float clampMin;
    @Deprecated
    public float clampMax;
}
