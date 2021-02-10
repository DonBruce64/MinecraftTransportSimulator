package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.BeaconManager;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockBeacon extends ABlockBaseDecor<TileEntityBeacon>{
	
    public BlockBeacon(){
    	super();
	}
    
    @Override
	public boolean onClicked(WrapperWorld world, Point3d position, Axis axis, WrapperPlayer player){
		if(world.isClient()){
			InterfaceGUI.openGUI(new GUITextEditor(world.getTileEntity(position)));
		}
		return true;
	}
    
    @Override
    public void onBroken(WrapperWorld world, Point3d position){
    	TileEntityBeacon beacon = world.getTileEntity(position);
    	if(beacon != null){
	    	BeaconManager.removeBeacon(world, beacon.beaconName);
    	}
    }
    
    @Override
	public TileEntityBeacon createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityBeacon(world, position, data);
	}

	@Override
	public Class<TileEntityBeacon> getTileEntityClass(){
		return TileEntityBeacon.class;
	}
}
