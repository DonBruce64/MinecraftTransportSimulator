package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockBeacon extends ABlockBaseTileEntity{
	
    public BlockBeacon(){
    	super(10.0F, 5.0F);
	}
    
    @Override
    public Class<TileEntityBeacon> getTileEntityClass(){
    	return TileEntityBeacon.class;
    }
    
    @Override
	public TileEntityBeacon createTileEntity(WrapperWorld world, Point3d position, WrapperPlayer placingPlayer, WrapperNBT data){
		return new TileEntityBeacon(world, position, placingPlayer, data);
	}
}
