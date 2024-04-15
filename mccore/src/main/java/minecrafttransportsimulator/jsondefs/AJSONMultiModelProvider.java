package minecrafttransportsimulator.jsondefs;

import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.rendering.RenderableData;

public abstract class AJSONMultiModelProvider extends AJSONItem {

    @JSONRequired
    @JSONDescription("A list of definitions for this content.  Each definition is simply a variant of a different texture on the same model, with potentially different names/descriptions/materials/etc.  If a component uses definitions, then you will need to specify at least one, even if the component only has one variant.  Also note that anything that has a definitions section is able to be added to dynamically via a skin.")
    public List<JSONSubDefinition> definitions;

    @JSONDescription("A mapping of constant-value variable names to values.  These variables will be added into the listing of active variables the moment the JSON is loaded. Note that they CAN be over-written if referenced as such, so keep this in mind.")
    public Map<String, Double> constantValues;

    @JSONDescription("A list of variables that will be set during initial placement of this object.  These can be used to set initial states, such as open doors or lights.")
    public List<String> initialVariables;

    @JSONDescription("A listing of variable modifiers.  These may be used to modify the vehicle's physics or other variables dynamically.  If present, these values will add-on to any defined value as the base, and will modify and keep the modification for any generic variable.")
    public List<JSONVariableModifier> variableModifiers;

    @JSONRequired
    @JSONDescription("The rendering properties for this object.")
    public JSONRendering rendering;

    @Deprecated
    public List<String> constants;

    /**
     * Returns the model location in the classpath for this definition.
     */
    public String getModelLocation(JSONSubDefinition subDefinition) {
        switch (rendering.modelType) {
            case OBJ:
                return PackResourceLoader.getPackResource(this, ResourceType.OBJ_MODEL, subDefinition.modelName != null ? subDefinition.modelName : systemName);
            case LITTLETILES:
                return PackResourceLoader.getPackResource(this, ResourceType.LT_MODEL, subDefinition.modelName != null ? subDefinition.modelName : systemName);
            case NONE:
                return null;
        }
        //We'll never get here.
        return null;
    }

    /**
     * Returns the OBJ model texture location in the classpath for this definition.
     * Sub-name is passed-in as different sub-names have different textures.
     */
    public String getTextureLocation(JSONSubDefinition subDefinition) {
        switch (rendering.modelType) {
            case OBJ:
                return PackResourceLoader.getPackResource(this, ResourceType.PNG, subDefinition.textureName != null ? subDefinition.textureName : systemName + subDefinition.subName);
            case LITTLETILES:
                return RenderableData.GLOBAL_TEXTURE_NAME;
            case NONE:
                return null;
        }
        //We'll never get here.
        return null;
    }
}