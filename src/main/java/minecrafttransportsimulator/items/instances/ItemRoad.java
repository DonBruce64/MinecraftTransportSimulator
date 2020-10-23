package minecrafttransportsimulator.items.instances;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockRoad;
import minecrafttransportsimulator.items.components.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;

public class ItemRoad extends ItemRoadComponent implements IItemBlock{
	
	public ItemRoad(JSONRoadComponent definition){
		super(definition);
	}
	
	@Override
	public Class<? extends ABlockBase> getBlockClass(){
		return BlockRoad.class;
	}
}
