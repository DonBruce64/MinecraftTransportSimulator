package minecrafttransportsimulator.items.packs;

import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.items.core.IItemBlock;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;

public class ItemPole extends ItemPoleComponent implements IItemBlock{
	
	public ItemPole(JSONPoleComponent definition){
		super(definition);
	}
	
	@Override
	public Class<? extends ABlockBase> getBlockClass(){
		return BlockPole.class;
	}
}
