package minecrafttransportsimulator.baseclasses;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketBeaconListingChange;

/**Class responsible for managing beacons in the world.  Handles access to beacons,
 * querying of beacon states, and saving/loading beacons from the world.
 * 
 * @author don_bruce
 */
public final class BeaconManager{
	private static final Map<Integer, Map<String, RadioBeacon>> worldBeacons = new HashMap<Integer, Map<String, RadioBeacon>>();
	
	/**
	 *  Returns the beacon with the specified name from the world, or null if it does not exist.
	 */
	public static RadioBeacon getBeacon(IWrapperWorld world, String name){
		if(!worldBeacons.containsKey(world.getDimensionID())){
			//No beacons for this world.  Load data.
			loadBeacons(world);
			
			//Check to make sure we actually loaded data before trying to get it.
			if(!worldBeacons.containsKey(world.getDimensionID())){
				return null;
			}
		}
		return worldBeacons.get(world.getDimensionID()).get(name);
	}
	
	/**
	 *  Adds the beacon with the specified name to the world.
	 */
	public static void addBeacon(IWrapperWorld world, RadioBeacon beacon){
		//Don't add un-named beacons.
		if(!beacon.name.isEmpty()){
			worldBeacons.get(world.getDimensionID()).put(beacon.name, beacon);
			if(!world.isClient()){
				saveBeacons(world);
				MasterLoader.networkInterface.sendToAllClients(new PacketBeaconListingChange(beacon));
			}
		}
	}
	
	/**
	 *  Removes the beacon with the specified name from the world.
	 */
	public static void removeBeacon(IWrapperWorld world, String name){
		worldBeacons.get(world.getDimensionID()).remove(name);
		if(!world.isClient()){
			saveBeacons(world);
			MasterLoader.networkInterface.sendToAllClients(new PacketBeaconListingChange(name));
		}
	}
	
	/**
	 *  Helper method to load beacon data from the world.
	 *  This may be called on either the client or the server,
	 *  however, the client may fail to load beacon data if it
	 *  hasn't gotten its data packet from the server yet.
	 */
	private static void loadBeacons(IWrapperWorld world){
		IWrapperNBT data = world.getData();
		if(data != null){
			Map<String, RadioBeacon> beacons = new HashMap<String, RadioBeacon>();
			int beaconCount = data.getInteger("radioBeaconCount");
			for(int i=0; i<beaconCount; ++i){
				RadioBeacon beacon = new RadioBeacon(data.getData("radioBeacon_" + i));
				beacons.put(beacon.name, beacon);
			}
			worldBeacons.put(world.getDimensionID(), beacons);
		}
	}
	
	/**
	 *  Helper method to save beacon data to the world.
	 *  Call this ONLY on the server.
	 */
	private static void saveBeacons(IWrapperWorld world){
		if(worldBeacons.containsKey(world.getDimensionID())){
			IWrapperNBT worldData = MasterLoader.coreInterface.createNewTag();
			int beaconIndex=0;
			for(RadioBeacon beacon : worldBeacons.get(world.getDimensionID()).values()){
				IWrapperNBT beaconData = MasterLoader.coreInterface.createNewTag();
				beacon.save(beaconData);
				worldData.setData("radioBeacon_" + beaconIndex++, beaconData);
			}
			worldData.setInteger("radioBeaconCount", beaconIndex);
			world.setData(worldData);
		}
	}
	
	public static class RadioBeacon{
		public final String name;
		public final double glideSlope;
		public final Point3i location;
		
		public RadioBeacon(IWrapperNBT data){
			this.name = data.getString("name");
			this.glideSlope = data.getInteger("glideSlope");
			this.location = data.getPoint3i("location");
		}
		
		public RadioBeacon(String name, double glideSlope, Point3i location){
			this.name = name;
			this.glideSlope = glideSlope;
			this.location = location;
		}
		
		public void save(IWrapperNBT data){
			data.setString("name", name);
			data.setDouble("glideSlope", glideSlope);
			data.setPoint3i("location", location);
		}
	}
}
