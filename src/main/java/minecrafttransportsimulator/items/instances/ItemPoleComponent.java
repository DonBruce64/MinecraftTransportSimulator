package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;

public class ItemPoleComponent extends AItemPack<JSONPoleComponent>{
	
	public ItemPoleComponent(JSONPoleComponent definition){
		super(definition);
	}
	
	@Override
	public String getModelLocation(){
		return definition.general.modelName != null ? "objmodels/poles/" + definition.general.modelName + ".obj" : "objmodels/poles/" + definition.systemName + ".obj";
	}
	
	@Override
	public String getTextureLocation(){
		return "textures/poles/" + definition.systemName + ".png";
	}
}
