package minecrafttransportsimulator.systems;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.baseclasses.NavBeacon;
import minecrafttransportsimulator.mcinterface.InterfacePacket;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketBeaconListingChange;

/**Class responsible for managing beacons in the world.  Handles access to beacons,
 * querying of beacon states, and saving/loading beacons from the world.
 * 
 * @author don_bruce
 */
public final class NavBeaconSystem{
	private static final Map<WrapperWorld, Map<String, NavBeacon>> worldBeacons = new HashMap<WrapperWorld, Map<String, NavBeacon>>();
	
	/**
	 *  Returns the beacon with the specified name from the world, or null if it does not exist.
	 */
	public static NavBeacon getBeacon(WrapperWorld world, String name){
		if(!worldBeacons.containsKey(world)){
			//No beacons for this world.  Load data.
			loadBeacons(world);
			
			//Check to make sure we actually loaded data before trying to get it.
			if(!worldBeacons.containsKey(world)){
				return null;
			}
		}
		return worldBeacons.get(world).get(name);
	}
	
	/**
	 *  Adds the beacon with the specified name to the world.
	 */
	public static void addBeacon(WrapperWorld world, NavBeacon beacon){
		//Don't add un-named beacons.
		if(!beacon.name.isEmpty()){
			worldBeacons.get(world).put(beacon.name, beacon);
			if(!world.isClient()){
				saveBeacons(world);
				InterfacePacket.sendToAllClients(new PacketBeaconListingChange(beacon));
			}
		}
	}
	
	/**
	 *  Removes the beacon with the specified name from the world.
	 */
	public static void removeBeacon(WrapperWorld world, String name){
		worldBeacons.get(world).remove(name);
		if(!world.isClient()){
			saveBeacons(world);
			InterfacePacket.sendToAllClients(new PacketBeaconListingChange(name));
		}
	}
	
	/**
	 *  Helper method to load beacon data from the world.
	 *  This may be called on either the client or the server,
	 *  however, the client may fail to load beacon data if it
	 *  hasn't gotten its data packet from the server yet.
	 */
	private static void loadBeacons(WrapperWorld world){
		WrapperNBT data = world.getData();
		if(data != null){
			Map<String, NavBeacon> beacons = new HashMap<String, NavBeacon>();
			int beaconCount = data.getInteger("radioBeaconCount");
			for(int i=0; i<beaconCount; ++i){
				NavBeacon beacon = new NavBeacon(data.getData("radioBeacon_" + i));
				beacons.put(beacon.name, beacon);
			}
			worldBeacons.put(world, beacons);
		}
	}
	
	/**
	 *  Helper method to save beacon data to the world.
	 *  Call this ONLY on the server.
	 */
	private static void saveBeacons(WrapperWorld world){
		if(worldBeacons.containsKey(world)){
			WrapperNBT worldData = new WrapperNBT();
			int beaconIndex=0;
			for(NavBeacon beacon : worldBeacons.get(world).values()){
				WrapperNBT beaconData = new WrapperNBT();
				beacon.save(beaconData);
				worldData.setData("radioBeacon_" + beaconIndex++, beaconData);
			}
			worldData.setInteger("radioBeaconCount", beaconIndex);
			world.setData(worldData);
		}
	}
}
