package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityChest;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockChest extends ABlockBaseDecor<TileEntityChest>{
	
	public BlockChest(){
		super();
	}

	@Override
	public TileEntityChest createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityChest(world, position, data);
	}

	@Override
	public Class<TileEntityChest> getTileEntityClass(){
		return TileEntityChest.class;
	}
}
