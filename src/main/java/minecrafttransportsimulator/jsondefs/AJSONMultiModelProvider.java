package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.rendering.components.RenderableObject;

public abstract class AJSONMultiModelProvider extends AJSONItem{

	@JSONRequired
	@JSONDescription("A list of definitions for this content.  Each definition is simply a variant of a different texture on the same model, with potentially different names/descriptions/materials/etc.  If a component uses definitions, then you will need to specify at least one, even if the component only has one variant.  Also note that anything that has a definitions section is able to be added to dynamically via a skin.")
	public List<JSONSubDefinition> definitions;
	@JSONDescription("The rendering properties for this object.")
    public JSONRendering rendering;
	
    /**
	 *  Returns the model location in the classpath for this definition.
	 */
	public String getModelLocation(String currentSubName){
		for(JSONSubDefinition subDefinition : definitions){
			if(subDefinition.subName.equals(currentSubName)){
				switch(rendering.modelType){
					case OBJ : return PackResourceLoader.getPackResource(this, ResourceType.OBJ_MODEL, subDefinition.modelName != null ? subDefinition.modelName : systemName);
					case LITTLETILES : return PackResourceLoader.getPackResource(this, ResourceType.LT_MODEL, subDefinition.modelName != null ? subDefinition.modelName : systemName);
					case NONE : return null;
				}
			}
		}
		//We'll never get here.
		return null;
	}
    
    /**
	 *  Returns the OBJ model texture location in the classpath for this definition.
	 *  Sub-name is passed-in as different sub-names have different textures.
	 */
	public String getTextureLocation(String currentSubName){
		for(JSONSubDefinition subDefinition : definitions){
			if(subDefinition.subName.equals(currentSubName)){
				switch(rendering.modelType){
					case OBJ : return PackResourceLoader.getPackResource(this, ResourceType.PNG, subDefinition.textureName != null ? subDefinition.textureName : systemName + currentSubName);
					case LITTLETILES : return RenderableObject.GLOBAL_TEXTURE_NAME;
					case NONE : return null;
				} 
			}
		}
		//We'll never get here.
		return null;
	}
}