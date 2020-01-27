package minecrafttransportsimulator.jsondefs;

public class JSONDecor extends AJSONMultiModel<JSONDecor.DecorGeneral>{

    public class DecorGeneral extends AJSONMultiModel.General{
    	public float width;
    	public float height;
    	public float depth;
    	public boolean oriented;
    	public boolean lighted;
    }
}