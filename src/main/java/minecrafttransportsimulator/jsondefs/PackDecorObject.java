package minecrafttransportsimulator.jsondefs;

public class PackDecorObject{
	public DecorGeneralConfig general;

    public class DecorGeneralConfig{
    	public float width;
    	public float height;
    	public float depth;
    	public boolean oriented;
    	public boolean lighted;
    	public String[] materials;
    	public String modelName;
    }
}