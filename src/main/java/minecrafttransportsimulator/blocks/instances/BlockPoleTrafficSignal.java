package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleTrafficSignal;

public class BlockPoleTrafficSignal extends BlockPole implements IBlockTileEntity{
	
	@Override
	public boolean canConnectOnFront(){
		return false;
	}

	@Override
	public TileEntityPoleTrafficSignal createTileEntity(){
		return new TileEntityPoleTrafficSignal();
	}
}
