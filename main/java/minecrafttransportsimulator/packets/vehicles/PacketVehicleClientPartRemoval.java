package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.parts.APart;

/**This packet is sent to clients when a part is removed from a vehicle.
 * This is only sent to clients tracking this vehicle, so we don't need
 * to do any null checks because we know they will have the vehicle and
 * thus must have the part as well.
 * 
 * @author don_bruce
 */
public class PacketVehicleClientPartRemoval extends APacketVehiclePart{

	public PacketVehicleClientPartRemoval(){}
	
	public PacketVehicleClientPartRemoval(APart part){
		super(part);
	}

	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		getVehicle( world).removePart(getPart(world), false);
	}
}
