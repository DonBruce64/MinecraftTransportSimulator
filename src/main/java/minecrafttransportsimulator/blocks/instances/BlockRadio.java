package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRadio;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockRadio extends ABlockBaseDecor<TileEntityRadio>{
	
	public BlockRadio(){
		super();
	}

	@Override
	public TileEntityRadio createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityRadio(world, position, data);
	}

	@Override
	public Class<TileEntityRadio> getTileEntityClass(){
		return TileEntityRadio.class;
	}
}
