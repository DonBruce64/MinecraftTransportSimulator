package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.packets.vehicles.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.parts.PartGroundDeviceWheel;


/**This packet is sent to clients when a wheel goes flat.
 * Causes clients to set the specified wheel as flat.
 * 
 * @author don_bruce
 */
public final class PacketPartClientWheelFlat extends APacketVehiclePart{
	public PacketPartClientWheelFlat(){}
	
	public PacketPartClientWheelFlat(PartGroundDeviceWheel part){
		super(part);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		((PartGroundDeviceWheel) getPart(world)).setFlat();
	}
}
