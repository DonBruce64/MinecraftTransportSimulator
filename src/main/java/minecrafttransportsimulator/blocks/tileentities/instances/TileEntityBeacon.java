package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BeaconManager;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.baseclasses.RadioBeacon;
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
	public static final int BEARING_INDEX = 2;
	
	public TileEntityBeacon(IWrapperWorld world, Point3i position, IWrapperNBT data){
		super(world, position, data);
		//Manually get the textLines, as these won't be in the JSON.
		textLines.clear();
		textLines.addAll(data.getStrings("textLines", 3));
		if(textLines.get(BEACON_NAME_INDEX).isEmpty()){
			//Need to add default entries as we don't have one.
			textLines.set(BEACON_NAME_INDEX, "NONE");
			textLines.set(GLIDE_SLOPE_INDEX, "10.0");
			textLines.set(BEARING_INDEX, "0.0");
		}
	}
	
	@Override
	public void setTextLines(List<String> textLinesToSet){
		BeaconManager.removeBeacon(world, textLines.get(BEACON_NAME_INDEX));
		super.setTextLines(textLinesToSet);
		try{
			RadioBeacon beacon = new RadioBeacon(textLinesToSet.get(BEACON_NAME_INDEX), Double.valueOf(textLinesToSet.get(GLIDE_SLOPE_INDEX)), Double.valueOf(textLinesToSet.get(BEARING_INDEX)), position);
			BeaconManager.addBeacon(world, beacon);
		}catch(Exception e){
			//Don't save this beacon.  It's entered invalid.
			textLines.set(BEACON_NAME_INDEX, "NONE");
			textLines.set(GLIDE_SLOPE_INDEX, "10.0");
			textLines.set(BEARING_INDEX, "0.0");
		}
	}
}
