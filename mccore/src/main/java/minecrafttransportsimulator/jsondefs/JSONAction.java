package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

public class JSONAction {
    @JSONDescription("The action to perform.")
    public ActionType action;

    @JSONDescription("The variable to perform the action on.")
    public String variable;

    @JSONDescription("The value to use in the action.")
    public double value;

    @JSONDescription("The min value for this action to set.  Only used when the action is increment.")
    public float clampMin;

    @JSONDescription("The max value for this action to set.  Only used when the action is increment.")
    public float clampMax;

    public static enum ActionType {
        @JSONDescription("Clicking this will toggle the variable from 0 to 1.")
        TOGGLE,
        @JSONDescription("Clicking this will set the variable to the defined value.")
        SET,
        @JSONDescription("Clicking this will increment the variable by the defined value.")
        INCREMENT,
        @JSONDescription("Clicking this will set the variable to the value. When the conditions for this action are false (letting go of the button, for example), it will be set back to 0.")
        BUTTON
    }
}
