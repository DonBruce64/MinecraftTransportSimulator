package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;

/**Packet used to send signals to ground devices.  Currently only used to make wheel-typed
 * ground devices flat on clients after they have been set as such on the server.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartGroundDevice extends APacketVehiclePart{
	final boolean flat;
	
	public PacketVehiclePartGroundDevice(PartGroundDevice groundDevice, boolean flat){
		super(groundDevice.vehicle, groundDevice.placementOffset);
		this.flat = flat;
	}
	
	public PacketVehiclePartGroundDevice(ByteBuf buf){
		super(buf);
		this.flat = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeBoolean(flat);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		PartGroundDevice groundDevice = (PartGroundDevice) vehicle.getPartAtLocation(offset);
		groundDevice.setFlatState(flat);
		return true;
	}
}
