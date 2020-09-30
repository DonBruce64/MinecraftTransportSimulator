package minecrafttransportsimulator.jsondefs;

public abstract class AJSONMultiModel<GeneralConfig extends AJSONMultiModel<GeneralConfig>.General> extends AJSONItem<GeneralConfig>{

    public class General extends AJSONItem<GeneralConfig>.General{
    	public String modelName;
    }
}