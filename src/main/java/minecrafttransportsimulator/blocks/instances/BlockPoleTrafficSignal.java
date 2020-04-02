package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleTrafficSignal;
import minecrafttransportsimulator.wrappers.WrapperBlockAxial;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;

public class BlockPoleTrafficSignal extends BlockPole implements WrapperTileEntity.IProvider{
	
	public BlockPoleTrafficSignal(WrapperBlockAxial wrapperReference){
		super(wrapperReference);
	}
	
	@Override
	public boolean canConnectOnFront(){
		return false;
	}

	@Override
	public TileEntityPoleTrafficSignal createTileEntity(){
		return new TileEntityPoleTrafficSignal();
	}
}
