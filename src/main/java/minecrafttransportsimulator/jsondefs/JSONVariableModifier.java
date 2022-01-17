package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

public class JSONVariableModifier{
	
	@JSONDescription("The name of the variable to modify.  You may also modify the property values in the vehicle motorized section.  Just use the same name.")
    public String variable;
	
	@JSONDescription("The value to add to the specified property.")
    public float value;
	
	@JSONDescription("A optional listing of animations used to decide when this modifier is active.  Visibiity animations will completely disable the modifier if they are false.  Translation animations will scale the modification.  Note that these values will apply on top of the existing value for the variable, AND the value parameter above.")
	public List<JSONAnimationDefinition> animations;
}
