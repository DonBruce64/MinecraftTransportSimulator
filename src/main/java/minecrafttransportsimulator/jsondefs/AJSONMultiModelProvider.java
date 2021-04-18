package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.APackResourceLoader.ResourceType;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;
import minecrafttransportsimulator.packloading.PackResourceLoaderDefault;

public abstract class AJSONMultiModelProvider extends AJSONItem{

	@JSONRequired
	@JSONDescription("A list of definitions for this content.  Each definition is simply a variant of a different texture on the same model, with potentially different names/descriptions/materials/etc.  If a component uses definitions, then you will need to specify at least one, even if the component only has one variant.  Also note that anything that has a definitions section is able to be added to dynamically via a skin.")
	public List<JSONSubDefinition> definitions;
	@JSONDescription("The rendering properties for this object.")
    public JSONRendering rendering;
	
    @SuppressWarnings("deprecation")
    /**
	 *  Returns the OBJ model location in the classpath for this definition.
	 */
	public String getModelLocation(){
		return PackResourceLoaderDefault.INSTANCE.getPackResource(this, ResourceType.OBJ, general.modelName != null ? general.modelName : systemName);
	}
    
    /**
	 *  Returns the OBJ model texture location in the classpath for this definition.
	 *  Sub-name is passed-in as different sub-names have different textures.
	 */
	public String getTextureLocation(String currentSubName){
		return PackResourceLoaderDefault.INSTANCE.getPackResource(this, ResourceType.PNG, systemName + currentSubName);
	}
}