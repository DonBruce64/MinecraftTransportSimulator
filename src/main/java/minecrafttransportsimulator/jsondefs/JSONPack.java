package minecrafttransportsimulator.jsondefs;

import java.util.List;

public class JSONPack{
	public boolean internallyGenerated;
	public String packID;
	public String packName;
	public int fileStructure;
	public String assetSubFolder;
	public List<List<String>> activatingSets;
	public List<List<String>> dependentSets;
	public List<String> recommendedPacks;
}
