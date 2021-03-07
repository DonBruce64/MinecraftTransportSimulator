package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.items.instances.ItemDecor.DecorComponentType;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

@JSONDescription("Sometimes, you just want to have something shiny to place in the world that can't be driven away by a pesky thief.  In this case, decor is the go-to choice.  Decors are essentially block-based OBJ models that can be placed down.  These come with full animation support, and can even have some additional functions built-in to justify the cost of crafting them.")
public class JSONDecor extends AJSONMultiModelProvider{
	
	@JSONDescription("Decor-specific properties.")
	public JSONDecorGeneric decor;

    public class JSONDecorGeneric{
		@JSONDescription("An optional type for this decor to give it functionality.")
    	public DecorComponentType type;
    	
		@JSONRequired
    	@JSONDescription("How wide a decor is.  1 is a full block width.  Numbers over 1 will result in unpredictable operations, so don't use them.")
    	public float width;
    	
		@JSONRequired
    	@JSONDescription("How high a decor is.  1 is a full block height.  Numbers over 1 will result in unpredictable operations, so don't use them.")
    	public float height;
    	
		@JSONRequired
		@JSONDescription("How deep a decor is.  1 is a full block depth.  Numbers over 1 will result in unpredictable operations, so don't use them.")
    	public float depth;
		
		@JSONDescription("A optional crafting definition for this decor.  If this is included, the decor will open a GUI for crafting pack components when clicked.")
		public JSONCraftingBench crafting;
    	
    	@Deprecated
    	public List<String> itemTypes;
    	@Deprecated
    	public List<String> partTypes;
    	@Deprecated
    	public List<String> items;
    }
}