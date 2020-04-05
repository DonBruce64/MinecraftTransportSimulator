package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleSign;

public class BlockPoleStreetlight extends BlockPole implements IBlockTileEntity{
    
	@Override
	public boolean canConnectOnFront(){
		return false;
	}

	@Override
	public TileEntityPoleSign createTileEntity(){
		return new TileEntityPoleSign();
	}
}
