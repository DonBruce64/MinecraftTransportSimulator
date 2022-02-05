package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntitySignalController;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockSignalController extends ABlockBaseTileEntity{
	
	public BlockSignalController(){
		super(10.0F, 5.0F);
	}
	
	@Override
    public Class<TileEntitySignalController> getTileEntityClass(){
    	return TileEntitySignalController.class;
    }

	@Override
	public TileEntitySignalController createTileEntity(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		return new TileEntitySignalController(world, position, placingPlayer, data);
	}
}
