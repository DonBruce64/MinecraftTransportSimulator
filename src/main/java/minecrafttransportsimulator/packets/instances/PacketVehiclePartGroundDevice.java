package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packets.components.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;

/**Packet used to send signals to ground devices.  Currently only used to make wheel-typed
 * ground devices flat on clients after they have been set as such on the server.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartGroundDevice extends APacketVehiclePart{
	
	public PacketVehiclePartGroundDevice(PartGroundDevice groundDevice){
		super(groundDevice.vehicle, groundDevice.placementOffset);
	}
	
	public PacketVehiclePartGroundDevice(ByteBuf buf){
		super(buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		PartGroundDevice groundDevice = (PartGroundDevice) vehicle.getPartAtLocation(offset);
		groundDevice.setFlat();
		return true;
	}
}
