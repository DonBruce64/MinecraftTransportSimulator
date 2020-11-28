package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.tileentities.components.BeaconManager;
import minecrafttransportsimulator.blocks.tileentities.components.BeaconManager.RadioBeacon;
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
	public static final int BEACON_NAME_INDEX = 0;
	public static final int GLIDE_SLOPE_INDEX = 1;
	
	public TileEntityBeacon(IWrapperWorld world, Point3i position, IWrapperNBT data){
		super(world, position, data);
		if(textLines.isEmpty()){
			//Need to add default text if we don't have any.
			textLines.add("");
			textLines.add("");
		}
	}
	
	@Override
	public void setTextLines(List<String> textLines){
		BeaconManager.removeBeacon(world, textLines.get(BEACON_NAME_INDEX));
		super.setTextLines(textLines);
		try{
			RadioBeacon beacon = new RadioBeacon(textLines.get(BEACON_NAME_INDEX), Integer.valueOf(textLines.get(GLIDE_SLOPE_INDEX)), position);
			BeaconManager.addBeacon(world, beacon);
		}catch(Exception e){
			//Don't save this beacon.  It's entered invalid.
			textLines.set(BEACON_NAME_INDEX, "");
			textLines.set(GLIDE_SLOPE_INDEX, "00000");
		}
	}
}
