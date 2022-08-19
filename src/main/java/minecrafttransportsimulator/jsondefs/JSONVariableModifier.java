package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONVariableModifier {

    @JSONRequired
    @JSONDescription("The name of the variable to modify.  You may also modify the property values in the vehicle motorized section.  Just use the same name.")
    public String variable;

    @JSONDescription("The value to add to the specified variable.")
    public float addValue;

    @JSONDescription("The value to set the specified variable to.  Overrides the current variable value and the addValue portion.  Does NOT override supplemental translation animations that may apply on top of this value.")
    public float setValue;

    @JSONDescription("The min value the variable will be assigned, after all operations are complete.")
    public float minValue;

    @JSONDescription("Like minValue, but max.")
    public float maxValue;

    @JSONDescription("A optional listing of animations used to decide when this modifier is active.  Visibiity animations will completely disable the modifier if they are false.  Translation transforms using the using the y-axis will add the value to the variable.  Translation transforms with the x-axis will multiply the value by the current variable value.  Translation transforms with the z-axis will set the variable to that value, overriding any prior transform operations.  Note that these values will apply on top of the existing value for the variable, PLUS the value parameter above (except z-axis set operations, of course).")
    public List<JSONAnimationDefinition> animations;
}
