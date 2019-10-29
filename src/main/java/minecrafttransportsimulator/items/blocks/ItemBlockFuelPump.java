package minecrafttransportsimulator.items.blocks;

import minecrafttransportsimulator.blocks.core.BlockFuelPump;
import net.minecraft.util.EnumFacing;

public class ItemBlockFuelPump extends ItemBlockRotatable{
	
	public ItemBlockFuelPump(){
		super();
	}
	
	@Override
	public ItemBlockRotatable createBlocks(){
		for(byte i=0; i<EnumFacing.HORIZONTALS.length; ++i){
			blocks[i] = new BlockFuelPump(EnumFacing.HORIZONTALS[i], this);
		}
		return this;
	}
}
