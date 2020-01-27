package minecrafttransportsimulator.jsondefs;

public abstract class AJSONMultiModel<GeneralConfig extends AJSONMultiModel.General> extends AJSONCraftable<GeneralConfig>{

    public class General extends AJSONCraftable.General{
    	public String modelName;
    }
}