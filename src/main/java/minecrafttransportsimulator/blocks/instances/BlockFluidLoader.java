package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.components.IBlockFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockFluidLoader extends ABlockBaseDecor<TileEntityFluidLoader> implements IBlockFluidTankProvider{
	
	public BlockFluidLoader(){
		super();
	}
	
    @Override
	public TileEntityFluidLoader createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityFluidLoader(world, position, data);
	}

	@Override
	public Class<TileEntityFluidLoader> getTileEntityClass(){
		return TileEntityFluidLoader.class;
	}
}
