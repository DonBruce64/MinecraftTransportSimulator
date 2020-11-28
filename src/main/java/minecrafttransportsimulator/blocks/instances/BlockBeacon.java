package minecrafttransportsimulator.blocks.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBaseDecor;
import minecrafttransportsimulator.blocks.tileentities.components.BeaconManager;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityBeacon;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.guis.instances.GUITextEditor;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;

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
    		List<String> textLines = beacon.getTextLines();
	    	BeaconManager.removeBeacon(world, textLines.get(TileEntityBeacon.BEACON_NAME_INDEX));
    	}
    }
    
    @Override
	public TileEntityBeacon createTileEntity(IWrapperWorld world, Point3i position, IWrapperNBT data){
		return new TileEntityBeacon(world, position, data);
	}

	@Override
	public Class<TileEntityBeacon> getTileEntityClass(){
		return TileEntityBeacon.class;
	}
}
