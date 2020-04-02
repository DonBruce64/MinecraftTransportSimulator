package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleCrossingSignal;
import minecrafttransportsimulator.wrappers.WrapperBlockAxial;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;

public class BlockPoleCrossingSignal extends BlockPole implements WrapperTileEntity.IProvider{
	
	public BlockPoleCrossingSignal(WrapperBlockAxial wrapperReference){
		super(wrapperReference);
	}
	
	@Override
	public boolean canConnectOnFront(){
		return false;
	}

	@Override
	public TileEntityPoleCrossingSignal createTileEntity(){
		return new TileEntityPoleCrossingSignal();
	}
}
