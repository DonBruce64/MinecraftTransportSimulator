package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONAutoGenerate;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONDecor extends AJSONMultiModelProvider<JSONDecor.DecorGeneral>{
	@JSONAutoGenerate
	@JSONDescription("Optional rendering properties for this decor.")
	public JSONRendering rendering;

    public class DecorGeneral extends AJSONMultiModelProvider<JSONDecor.DecorGeneral>.General{
		@JSONDescription("An optional type for this decor to give it functionality.  Valid functions are: [beacon, fuel_pump, fluid_loader, signal_controller, radio]")
    	public String type;
    	
		@JSONRequired
    	@JSONDescription("How wide a decor is.  1 is a full block width.  Numbers over 1 will result in unpredictable operations, so don't use them.")
    	public float width;
    	
		@JSONRequired
    	@JSONDescription("How high a decor is.  1 is a full block height.  Numbers over 1 will result in unpredictable operations, so don't use them.")
    	public float height;
    	
		@JSONRequired
		@JSONDescription("How deep a decor is.  1 is a full block depth.  Numbers over 1 will result in unpredictable operations, so don't use them.")
    	public float depth;
    	public TextLine[] textLines;
    	@Deprecated
    	public List<JSONText> textObjects;
    	
    	@JSONDescription("A optional list of item types this decor can craft.  This is the first layer of filtering, and restricts the items displayed to only crafting items that have the same type.  Valid types correspond to JSON definition types, those being the types that you make sub-folders for in your pack.")
    	public List<String> itemTypes;
    	
    	@JSONDescription("A optional list of part types this decor can craft.  Only used to filter “part” itemTypes.  For example, adding “engine” would allow the decor to craft all engines, but adding “engine_car” and “engine_boat” would prevent it from crafting aircraft engines.")
    	public List<String> partTypes;
    	
    	@JSONDescription("A list of items this decor may craft.  This overrides all other filters, and may be used to specify exactly what this decor may craft.  The format for this is [packID:systemName], where systemName is the name of the item with the subName appended.")
    	public List<String> items;
    }
    
    @Deprecated
    public class TextLine{
    	public float xPos;
    	public float yPos;
    	public float zPos;
    	public float scale;
    	public String color;
    }
}