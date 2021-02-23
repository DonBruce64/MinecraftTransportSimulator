package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockFluidLoader extends ABlockBaseDecor<TileEntityFluidLoader>{
	
	public BlockFluidLoader(){
		super();
	}
	
	@Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
		return false;
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
