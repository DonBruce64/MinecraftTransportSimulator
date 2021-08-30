package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.systems.NavBeaconSystem;

public class BlockBeacon extends ABlockBaseDecor{
	
    public BlockBeacon(){
    	super();
	}
    
    @Override
    public void onBroken(WrapperWorld world, Point3d position){
    	TileEntityBeacon beacon = world.getTileEntity(position);
    	if(beacon != null){
	    	NavBeaconSystem.removeBeacon(world, beacon.beaconName);
    	}
    }
    
    @Override
	public TileEntityBeacon createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityBeacon(world, position, data);
	}
}
