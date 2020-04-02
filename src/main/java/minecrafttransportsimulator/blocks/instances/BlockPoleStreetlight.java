package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleSign;
import minecrafttransportsimulator.wrappers.WrapperBlockAxial;
import minecrafttransportsimulator.wrappers.WrapperTileEntity;

public class BlockPoleStreetlight extends BlockPole implements WrapperTileEntity.IProvider{
    	
	public BlockPoleStreetlight(WrapperBlockAxial wrapperReference){
		super(wrapperReference);
	}
	
	@Override
	public boolean canConnectOnFront(){
		return false;
	}

	@Override
	public TileEntityPoleSign createTileEntity(){
		return new TileEntityPoleSign();
	}
}
