package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONAnimatedObject{
	@JSONRequired
	public String objectName;
	public String applyAfter;
	@JSONRequired
	public List<JSONAnimationDefinition> animations;
}
