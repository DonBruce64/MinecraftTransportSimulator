package minecrafttransportsimulator.jsondefs;

public abstract class AJSONItem<GenralConfig extends AJSONItem.General>{
	public GenralConfig general;

    public class General{
    	public String name;
    }
}