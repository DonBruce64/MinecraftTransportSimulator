package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.BeaconManager;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.mcinterface.InterfaceGUI;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockBeacon extends ABlockBaseDecor<TileEntityBeacon>{
	
    public BlockBeacon(){
    	super();
	}
    
    @Override
	public boolean onClicked(WrapperWorld world, Point3i point, Axis axis, WrapperPlayer player){
		if(world.isClient()){
			InterfaceGUI.openGUI(new GUITextEditor((TileEntityDecor) world.getTileEntity(point)));
		}
		return true;
	}
    
    @Override
    public void onBroken(WrapperWorld world, Point3i location){
    	TileEntityBeacon beacon = world.getTileEntity(location);
    	if(beacon != null){
	    	BeaconManager.removeBeacon(world, beacon.beaconName);
    	}
    }
    
    @Override
	public TileEntityBeacon createTileEntity(WrapperWorld world, Point3i position, WrapperNBT data){
		return new TileEntityBeacon(world, position, data);
	}

	@Override
	public Class<TileEntityBeacon> getTileEntityClass(){
		return TileEntityBeacon.class;
	}
}
