package minecrafttransportsimulator.jsondefs;

public abstract class AJSONCraftable<GeneralConfig extends AJSONCraftable.General> extends AJSONItem<GeneralConfig>{

    public class General extends AJSONItem.General{
    	public String[] materials;
    }
}