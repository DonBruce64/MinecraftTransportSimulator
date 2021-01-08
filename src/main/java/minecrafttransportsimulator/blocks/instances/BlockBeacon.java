package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.BeaconManager;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.mcinterface.WrapperNBT;

public class BlockBeacon extends ABlockBaseDecor<TileEntityBeacon>{
	
    public BlockBeacon(){
    	super();
	}
    
    @Override
	public boolean onClicked(IWrapperWorld world, Point3i point, Axis axis, IWrapperPlayer player){
		if(world.isClient()){
			MasterLoader.guiInterface.openGUI(new GUITextEditor((TileEntityDecor) world.getTileEntity(point)));
		}
		return true;
	}
    
    @Override
    public void onBroken(IWrapperWorld world, Point3i location){
    	TileEntityBeacon beacon = world.getTileEntity(location);
    	if(beacon != null){
	    	BeaconManager.removeBeacon(world, beacon.beaconName);
    	}
    }
    
    @Override
	public TileEntityBeacon createTileEntity(IWrapperWorld world, Point3i position, WrapperNBT data){
		return new TileEntityBeacon(world, position, data);
	}

	@Override
	public Class<TileEntityBeacon> getTileEntityClass(){
		return TileEntityBeacon.class;
	}
}
