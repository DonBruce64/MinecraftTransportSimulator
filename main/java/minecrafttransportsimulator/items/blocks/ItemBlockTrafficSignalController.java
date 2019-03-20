package minecrafttransportsimulator.items.blocks;

import minecrafttransportsimulator.blocks.core.BlockTrafficSignalController;
import net.minecraft.util.EnumFacing;

public class ItemBlockTrafficSignalController extends ItemBlockRotatable{
	
	@Override
	public ItemBlockRotatable createBlocks(){
		for(byte i=0; i<EnumFacing.HORIZONTALS.length; ++i){
			blocks[i] = new BlockTrafficSignalController(EnumFacing.HORIZONTALS[i], this);
		}
		return this;
	}
}
