package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;

/**Beacon tile entity.  Contains code for handling interfacing with
 * the global world saved data and information of the beacon states.
 * Note that the variables for this beacon are saved as textLines
 * in the main decor class to ensure they both display properly,
 * and to allow a common interface for all GUI operations.
 *
 * @author don_bruce
 */
public class TileEntityBeacon extends TileEntityDecor{
	private static final int BEACON_NAME_INDEX = 0;
	private static final int GLIDE_SLOPE_INDEX = 0;
	
	public TileEntityBeacon(IWrapperWorld world, Point3i position, IWrapperNBT data){
		super(world, position, data);
		if(textLines.isEmpty()){
			textLines.add("");
			textLines.add("");
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		String beaconName = textLines.get(BEACON_NAME_INDEX);
		if(!beaconName.isEmpty() && !world.isClient()){
			IWrapperNBT worldData = world.getData();
			List<String> beaconNames = worldData.getStrings("beaconNames", worldData.getInteger("totalBeacons"));
			beaconNames.remove(beaconName);
			worldData.setStrings("beaconNames", beaconNames);
			worldData.deleteData("beaconLocation_" + beaconName);
			worldData.deleteData("beaconGlideSlope_" + beaconName);
			world.setData(worldData);
			textLines.set(BEACON_NAME_INDEX, "");
			textLines.set(GLIDE_SLOPE_INDEX, "");
		}
	}
	
	@Override
	public void setTextLines(List<String> textLines){
		super.setTextLines(textLines);
		String beaconName = textLines.get(BEACON_NAME_INDEX);
		if(!beaconName.isEmpty() && !world.isClient()){
			IWrapperNBT worldData = world.getData();
			List<String> beaconNames = worldData.getStrings("beaconNames", worldData.getInteger("totalBeacons"));
			beaconNames.add(beaconName);
			worldData.setStrings("beaconNames", beaconNames);
			worldData.setPoint3i("beaconLocation_" + beaconName, position);
			worldData.setInteger("beaconGlideSlope_" + beaconName, Integer.valueOf(textLines.get(GLIDE_SLOPE_INDEX)));
			world.setData(worldData);
		}
	}
}
