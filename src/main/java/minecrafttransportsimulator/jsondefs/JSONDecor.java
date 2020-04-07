package minecrafttransportsimulator.jsondefs;

public class JSONDecor extends AJSONMultiModel<JSONDecor.DecorGeneral>{

    public class DecorGeneral extends AJSONMultiModel<JSONDecor.DecorGeneral>.General{
    	public String type;
    	public float width;
    	public float height;
    	public float depth;
    	public String[] fuelTypes;
    }
}