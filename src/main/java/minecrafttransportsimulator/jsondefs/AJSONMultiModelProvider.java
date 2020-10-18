package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;

public abstract class AJSONMultiModelProvider<GeneralConfig extends AJSONMultiModelProvider<GeneralConfig>.General> extends AJSONItem<GeneralConfig>{

	public List<SubDefinition> definitions;
	
    public class General extends AJSONItem<GeneralConfig>.General{
    	@Deprecated
    	public String modelName;
    }
    
    public class SubDefinition{
    	public String subName;
    	public String secondTone;
    	public String secondColor;
    	public String name;
    	public String[] extraMaterials;
    }
	
    public String getModelLocation(){
		return PackResourceLoader.getPackResource(this, ResourceType.OBJ, general.modelName != null ? general.modelName : systemName);
	}
    
	public String getTextureLocation(String currentSubName){
		return PackResourceLoader.getPackResource(this, ResourceType.PNG, systemName + currentSubName);
	}
}