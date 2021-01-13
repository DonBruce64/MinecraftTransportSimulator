package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONSubDefinition{
	@JSONRequired
	public String subName;
	public String secondTone;
	public String secondColor;
	@JSONRequired
	public String name;
	public String description;
	@JSONRequired
	public List<String> extraMaterials;
}
