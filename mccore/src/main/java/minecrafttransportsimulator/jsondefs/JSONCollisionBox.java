package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.baseclasses.Point3D;
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

    @JSONDescription("How much armor this collision box has.  Values greater than 0 will make this box use armor code to block bullets from passing through it.  Leaving this value out will make all bullets pass through it (no armor).")
    public float armorThickness;

    @JSONDescription("Like normal armor thickness, but used only if a bullet is a HEAT bullet.")
    public float heatArmorThickness;

    @JSONDescription("How much to multiply the damage for bullets that hit this hitbox by.  Is 1.0 if not specified.")
    public float damageMultiplier;

    @JSONDescription("If set, clicking this collision box will do variable operations.  The exact operation depends on the variableType and variableValue.  Useful for doors, though can be used for any toggle-able variable, not just custom doors.")
    public String variableName;

    @JSONRequired(dependentField = "variableName")
    @JSONDescription("The type of variable this box represents, and how to act when clicked.")
    public VariableType variableType;

    @JSONDescription("The value to set the variable to for SET types, or the amount to increment by for INCREMENT types.")
    public float variableValue;

    @JSONDescription("The min value for this box to set.  Only used when the variable type is increment.")
    public float clampMin;

    @JSONDescription("The max value for this box to set.  Only used when the variable type is increment.")
    public float clampMax;

    public enum VariableType {
        @JSONDescription("Clicking this box will toggle the variable from 0 to 1.")
        TOGGLE,
        @JSONDescription("Clicking this box will set the variable to the defined value.")
        SET,
        @JSONDescription("Clicking this box will increment the variable by the defined value.")
        INCREMENT,
        @JSONDescription("Clicking this box will set the variable to the value. When the player lets go of the mouse button, it will be set back to 0.")
        BUTTON
    }

    @Deprecated
    public boolean isInterior;
}
