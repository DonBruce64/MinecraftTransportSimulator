package minecrafttransportsimulator.jsondefs;

public class JSONDecor extends AJSONCraftable<JSONDecor.DecorGeneral>{

    public class DecorGeneral extends AJSONCraftable.General{
    	public float width;
    	public float height;
    	public float depth;
    	public boolean oriented;
    	public boolean lighted;
    	public String modelName;
    }
}