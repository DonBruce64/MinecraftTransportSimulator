package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityChest;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockChest extends ABlockBaseTileEntity{
	
	public BlockChest(){
		super(10.0F, 5.0F);
	}
	
	@Override
    public Class<TileEntityChest> getTileEntityClass(){
    	return TileEntityChest.class;
    }

	@Override
	public TileEntityChest createTileEntity(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		return new TileEntityChest(world, position, placingPlayer, data);
	}
}
