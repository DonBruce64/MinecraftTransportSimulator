package minecrafttransportsimulator.items.blocks;

import minecrafttransportsimulator.blocks.core.BlockBench;
import net.minecraft.util.EnumFacing;

public class ItemBlockBench extends ItemBlockRotatable{
	private final String[] partTypes;
	
	public ItemBlockBench(String... partTypes){
		super();
		this.partTypes = partTypes;
	}
	
	@Override
	public ItemBlockRotatable createBlocks(){
		for(byte i=0; i<EnumFacing.HORIZONTALS.length; ++i){
			blocks[i] = new BlockBench(EnumFacing.HORIZONTALS[i], this, partTypes);
		}
		return this;
	}
}
