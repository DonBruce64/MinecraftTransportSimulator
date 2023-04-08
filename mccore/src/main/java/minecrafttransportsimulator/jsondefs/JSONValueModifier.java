package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONValueModifier {

    @JSONRequired
    @JSONDescription("The blockType for this code.  The type defines the logic to be performed.")
    public Type type;

    @JSONRequired(dependentField = "type", dependentValues = { "SET_VAR", "ADD_VAR", "MULTIPLY_VAR", "LINEAR", "PARABOLIC" })
    @JSONDescription("")
    public String input;
    
    @JSONDescription("The factor to apply to this operation (if used).  Can be left out to not do any factoring (defaults to 1).")
    public float factor;

    @JSONDescription("")
    public float parameter1;

    @JSONDescription("")
    public float parameter2;

    @JSONDescription("")
    public float parameter3;

    @JSONDescription(".")
    public float parameter4;

    @JSONRequired(dependentField = "type", dependentValues = { "CONDITIONS" })
    @JSONDescription("The conditions to use for the conditions type.")
    public JSONConditionGroup conditions;

    @JSONDescription("The code to run if the block is a conditional, and is true.")
    public List<JSONValueModifier> trueCode;

    @JSONDescription("The code to run if the block is a conditional, and is false.")
    public List<JSONValueModifier> falseCode;

    public static enum Type {
        @JSONDescription("value = parameter1")
        SET,
        @JSONDescription("value = input*factor")
        SET_VAR,
        @JSONDescription("value = value + parameter1")
        ADD,
        @JSONDescription("value = value + input*factor")
        ADD_VAR,
        @JSONDescription("value = value * parameter1")
        MULTIPLY,
        @JSONDescription("value = value * input*factor")
        MULTIPLY_VAR,
        @JSONDescription("value = input * parameter1 + paramter2")
        LINEAR,
        @JSONDescription("value = parameter1 * (input * paramter2 - paramter3)^2 + parameter4.")
        PARABOLIC,
        @JSONDescription("If value < parameter1, then value = parameter1, eise if value > parameter2 then value = parameter2.")
        CLAMP,
        @JSONDescription("If all conditions are true, then trueCode, else falseCode.")
        CONDITIONS;
    }
}
