package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONConditionGroup {

    @JSONRequired
    @JSONDescription("The conditions for this group.")
    public List<JSONCondition> conditions;

    @JSONDescription("The delay, in ticks, that all these conditions must be true for the result to change from false to true.")
    public int onDelay;

    @JSONDescription("The delay, in ticks, that all these conditions must be false for the result to change from true to false.")
    public int offDelay;
}
