package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleCrossingSignal;

public class BlockPoleCrossingSignal extends BlockPole implements IBlockTileEntity{
	
	@Override
	public boolean canConnectOnFront(){
		return false;
	}

	@Override
	public TileEntityPoleCrossingSignal createTileEntity(){
		return new TileEntityPoleCrossingSignal();
	}
}
