package minecrafttransportsimulator.jsondefs;

import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;
import minecrafttransportsimulator.packloading.JSONParser.JSONRequired;

public class JSONPoleComponent extends AJSONMultiModelProvider{
	
	@JSONDescription("Pole-specific properties.")
	public JSONPoleGeneric pole;
	
    public class JSONPoleGeneric{
    	@JSONRequired
    	@JSONDescription("The type of this pole.  This defines its properties.")
    	public PoleComponentType type;
    	
    	@JSONRequired
    	@JSONDescription("This parameter tells MTS how much to offset components put on this pole.\nThis is because some poles may be larger than others, and making it so models always render at the same point would lead to clipping on large poles and floating on small ones. \nFor all cases, you should set this to the offset from the center where all components should attach to your pole.")
    	public float radius;
    }
}
