package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.BeaconManager;
import minecrafttransportsimulator.baseclasses.RadioBeacon;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

/**This packet is sent from the server to all clients when the list of beacons changes.  This
 * allows clients to stay in-sync with the beacons currently in the world.
 * 
 * @author don_bruce
 */
public class PacketBeaconListingChange extends APacketBase{
	private final String beaconName;
	private final RadioBeacon beacon;
	
	public PacketBeaconListingChange(String beaconName){
		super(null);
		this.beaconName = beaconName;
		this.beacon = null;
	}
	
	public PacketBeaconListingChange(RadioBeacon beacon){
		super(null);
		this.beaconName = null;
		this.beacon = beacon;
	}
	
	public PacketBeaconListingChange(ByteBuf buf){
		super(buf);
		if(buf.readBoolean()){
			this.beaconName = readStringFromBuffer(buf);
			this.beacon = null;
		}else{
			this.beaconName = null;
			this.beacon = new RadioBeacon(readDataFromBuffer(buf));
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		if(beaconName != null){
			buf.writeBoolean(true);
			writeStringToBuffer(beaconName, buf);
		}else{
			buf.writeBoolean(false);
			WrapperNBT data = new WrapperNBT();
			beacon.save(data);
			writeDataToBuffer(data, buf);
		}
	}
	
	@Override
	public void handle(WrapperWorld world){
		if(beaconName != null){
			BeaconManager.removeBeacon(world, beaconName);
		}else{
			BeaconManager.addBeacon(world, beacon);
		}
	}
}
