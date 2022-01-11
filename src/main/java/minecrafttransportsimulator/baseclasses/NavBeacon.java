package minecrafttransportsimulator.baseclasses;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityB_Existing;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Beacon class.  Responsible for containing the state of a beacon, which includes
 * the beacon's position, and other properties.
 * 
 * @author don_bruce
 */
public class NavBeacon{
	private static final String BEACON_LISTING_KEY = "beacons";
	private static final Map<WrapperWorld, Map<String, NavBeacon>> cachedBeaconMaps = new HashMap<WrapperWorld, Map<String, NavBeacon>>();
	
	public final String name;
	public final double glideSlope;
	public final double bearing;
	public final Point3d position;
	
	public static NavBeacon getByNameFromWorld(WrapperWorld world, String name){
		if(!cachedBeaconMaps.containsKey(world)){
			WrapperNBT beaconListing = world.getData(BEACON_LISTING_KEY);
			if(beaconListing != null){
				Map<String, NavBeacon> beaconMap = new HashMap<String, NavBeacon>();
				for(String beaconName : beaconListing.getAllNames()){
					beaconMap.put(beaconName, new NavBeacon(beaconListing.getData(beaconName)));
				}
				cachedBeaconMaps.put(world, beaconMap);
			}else{
				return null;
			}
		}
		
		return cachedBeaconMaps.get(world).get(name);
	}
	
	public static void removeFromWorld(WrapperWorld world, String name){
		if(name != null){
			if(cachedBeaconMaps.containsKey(world)){
				cachedBeaconMaps.get(world).remove(name);
			}
			
			WrapperNBT beaconListing = world.getData(BEACON_LISTING_KEY);
			if(beaconListing != null){
				beaconListing.deleteData(name);
				world.setData(BEACON_LISTING_KEY, beaconListing);
			}
		}
	}
	
	private NavBeacon(WrapperNBT data){
		this.name = data.getString("name");
		this.glideSlope = data.getDouble("glideSlope");
		this.bearing = data.getDouble("bearing");
		this.position = data.getPoint3dCompact("location");
	}
	
	public NavBeacon(WrapperWorld world, String name, double glideSlope, double bearing, Point3d position){
		this.name = name;
		this.glideSlope = glideSlope;
		this.bearing = bearing;
		this.position = position;
		WrapperNBT beaconListing = world.getData(BEACON_LISTING_KEY);
		if(beaconListing == null){
			beaconListing = new WrapperNBT();
		}
		beaconListing.setData(name, save(new WrapperNBT()));
		world.setData(BEACON_LISTING_KEY, beaconListing);	
	}
	
	public double getBearingDelta(AEntityB_Existing entity){
		//Add 180 to the bearing, as players enter the direction to land, but the bearing delta needs to return the
		//delta between the entity and the beacon's "beam".  This requires the beam to be opposite of the landing direction.
		//Normally we'd subtract the bearing here, but MC inverts y-coords, so players enter the bearing backwards.
		double delta = Math.toDegrees(Math.atan2(entity.position.x - position.x, entity.position.z - position.z)) + bearing + 180;
		while(delta < -180) delta += 360;
		while(delta > 180) delta -= 360;
		return delta;
	}
	
	public WrapperNBT save(WrapperNBT data){
		data.setString("name", name);
		data.setDouble("glideSlope", glideSlope);
		data.setDouble("bearing", bearing);
		data.setPoint3dCompact("location", position);
		return data;
	}
}