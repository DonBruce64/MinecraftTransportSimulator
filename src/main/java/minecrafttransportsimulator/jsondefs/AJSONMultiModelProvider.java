package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;

public abstract class AJSONMultiModelProvider extends AJSONItem{

	@JSONRequired
	@JSONDescription("A list of definitions for this content.  Each definition is simply a variant of a different texture on the same model, with potentially different names/descriptions/materials/etc.  If a component uses definitions, then you will need to specify at least one, even if the component only has one variant.  Also note that anything that has a definitions section is able to be added to dynamically via a skin.")
	public List<JSONSubDefinition> definitions;
	@JSONRequired
	@JSONDescription("The rendering properties for this object.")
    public JSONRendering rendering;
	
    @SuppressWarnings("deprecation")
	public String getModelLocation(){
		return PackResourceLoader.getPackResource(this, ResourceType.OBJ, general.modelName != null ? general.modelName : systemName);
	}
    
	public String getTextureLocation(String currentSubName){
		return PackResourceLoader.getPackResource(this, ResourceType.PNG, systemName + currentSubName);
	}
}