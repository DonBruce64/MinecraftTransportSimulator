package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.components.IBlockFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockFuelPump extends ABlockBaseDecor<TileEntityFuelPump> implements IBlockFluidTankProvider{
	
	public BlockFuelPump(){
		super();
	}
	
	@Override
	public void onPlaced(WrapperWorld world, Point3d position, WrapperPlayer player){
		//Set placing player for reference.
		TileEntityFuelPump pump = world.getTileEntity(position);
		pump.placingPlayerID = player.getID();
	}
	
    @Override
	public TileEntityFuelPump createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityFuelPump(world, position, data);
	}

	@Override
	public Class<TileEntityFuelPump> getTileEntityClass(){
		return TileEntityFuelPump.class;
	}
}
