package minecrafttransportsimulator.jsondefs;

import java.util.List;
import java.util.Map;

public class JSONPack{
	public boolean internallyGenerated;
	public String packID;
	public String packName;
	public int fileStructure;
	public Map<String, List<String>> activators;
	public Map<String, List<String>> blockers;
	public List<String> dependents;
}
