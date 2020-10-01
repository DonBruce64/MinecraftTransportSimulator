package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;

public abstract class AJSONModelProvider<GeneralConfig extends AJSONModelProvider<GeneralConfig>.General> extends AJSONItem<GeneralConfig>{

    public class General extends AJSONItem<GeneralConfig>.General{
    	public String modelName;
    }
    
	public String getModelLocation(){
		return PackResourceLoader.getPackResource(this, ResourceType.OBJ, general.modelName != null ? general.modelName : systemName);
	}
	
	public String getTextureLocation(){
		return PackResourceLoader.getPackResource(this, ResourceType.PNG, systemName);
	}
}