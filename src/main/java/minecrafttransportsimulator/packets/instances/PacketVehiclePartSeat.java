package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.PartSeat;

/**Packet used to send signals to seats.  Currently, this is only used to change the currently-selected
 * gun that the seat is set to fire.  Sent to servers when a player presses the gun key.  Sent back to all
 * clients for updating.  May also be sent from the server to all clients when a seat is first entered.
 * 
 * @author don_bruce
 */
public class PacketVehiclePartSeat extends APacketVehiclePart{
	
	public PacketVehiclePartSeat(PartSeat seat){
		super(seat.vehicle, seat.placementOffset);
	}
	
	public PacketVehiclePartSeat(ByteBuf buf){
		super(buf);
	}
	
	@Override
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle, Point3d offset){
		((PartSeat) vehicle.getPartAtLocation(offset)).setNextActiveGun();
		return true;
	}
}
