package minecrafttransportsimulator.jsondefs;

public abstract class AJSONMultiModel<GeneralConfig extends AJSONMultiModel<GeneralConfig>.General> extends AJSONCraftable<GeneralConfig>{

    public class General extends AJSONCraftable<GeneralConfig>.General{
    	public String modelName;
    }
}