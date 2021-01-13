package minecrafttransportsimulator.jsondefs;

import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONPack{
	public boolean internallyGenerated;
	@JSONRequired
	public String packID;
	@JSONRequired
	public String packName;
	public int fileStructure;
	public Map<String, List<String>> activators;
	public Map<String, List<String>> blockers;
	public List<String> dependents;
}
