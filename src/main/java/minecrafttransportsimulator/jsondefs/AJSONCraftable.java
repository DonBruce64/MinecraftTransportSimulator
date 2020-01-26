package minecrafttransportsimulator.jsondefs;

public abstract class AJSONCraftable<GenralConfig extends AJSONCraftable.General> extends AJSONItem<GenralConfig>{

    public class General extends AJSONItem.General{
    	public String[] materials;
    }
}