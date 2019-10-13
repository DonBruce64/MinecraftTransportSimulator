package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;


/**This packet is sent to clients when a vehicle is removed on a server.
 * We won't need to remove the vehicle as MC takes care of removed
 * entities for us.  What we send this for is to tell the SFXSystem
 * to stop playing all sounds for this Entity.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientRemoval extends APacketVehicle{

	public PacketVehicleClientRemoval(){}
	
	public PacketVehicleClientRemoval(EntityVehicleE_Powered vehicle){
		super(vehicle);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, MTSPlayerInterface player, boolean onServer){
		SFXSystem.stopVehicleSounds((EntityVehicleE_Powered) getVehicle(world));
	}
}
