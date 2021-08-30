package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.components.IBlockFluidTankProvider;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFuelPump;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockFuelPump extends ABlockBaseTileEntity implements IBlockFluidTankProvider{
	
	public BlockFuelPump(){
		super(10.0F, 5.0F);
	}
	
    @Override
	public TileEntityFuelPump createTileEntity(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		return new TileEntityFuelPump(world, position, placingPlayer, data);
	}
}
