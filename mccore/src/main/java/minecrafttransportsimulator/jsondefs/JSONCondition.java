package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONCondition {
    public transient int delay;

    @JSONRequired
    @JSONDescription("The type for this condition.")
    public Type type;

    @JSONRequired(dependentField = "type", dependentValues = { "MATCH", "GREATER", "BOUNDS" })
    @JSONDescription("")
    public String input;

    @JSONDescription("")
    public String variable1;

    @JSONDescription("")
    public String variable2;

    @JSONDescription("")
    public float parameter1;

    @JSONDescription("")
    public float parameter2;

    @JSONRequired(dependentField = "type", dependentValues = { "CONDITIONS" })
    @JSONDescription("The conditions to use for the conditions type.")
    public List<JSONCondition> conditions;

    @JSONDescription("If true, inverts the logic of this conditional.")
    public boolean invert;

    @JSONDescription("The delay, in ticks, that this condition must be true for the result to change from false to true.")
    public int onDelay;

    @JSONDescription("The delay, in ticks, that this condition must be false for the result to change from true to false.")
    public int offDelay;

    public static enum Type {
        @JSONDescription("input > 0")
        ACTIVE,
        @JSONDescription("input == paramter1")
        MATCH,
        @JSONDescription("input == variable1")
        MATCH_VAR,
        @JSONDescription("input > paramter1")
        GREATER,
        @JSONDescription("input > variable1")
        GREATER_VAR,
        @JSONDescription("input < paramter1")
        LESS,
        @JSONDescription("input < variable1")
        LESS_VAR,
        @JSONDescription("input >= paramter1 AND variable1 <= parameter2")
        BOUNDS,
        @JSONDescription("input >= variable1 AND input <= variable2")
        BOUNDS_VAR,
        @JSONDescription("At least one of the conditions must be true.")
        CONDITIONS;
    }
}
