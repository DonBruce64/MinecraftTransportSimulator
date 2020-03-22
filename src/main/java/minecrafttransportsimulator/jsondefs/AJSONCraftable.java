package minecrafttransportsimulator.jsondefs;

public abstract class AJSONCraftable<GeneralConfig extends AJSONCraftable<GeneralConfig>.General> extends AJSONItem<GeneralConfig>{

    public class General extends AJSONItem<GeneralConfig>.General{
    	public String[] materials;
    }
}