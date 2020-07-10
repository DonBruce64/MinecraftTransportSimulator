package minecrafttransportsimulator.packets.components;

import io.netty.buffer.ByteBuf;
import mcinterface.InterfaceNetwork;
import mcinterface.WrapperEntityPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet class that includes a default implementation for transmitting a vehicle
 * to allow vehicle-specific interactions on the other side of the network.
 *
 * @author don_bruce
 */
public abstract class APacketVehicle extends APacketBase{
	private final int vehicleID;
	
	public APacketVehicle(EntityVehicleF_Physics vehicle){
		super(null);
		this.vehicleID = vehicle.uniqueID;
	}
	
	public APacketVehicle(ByteBuf buf){
		super(buf);
		this.vehicleID = buf.readInt();
	};

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(vehicleID);
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperEntityPlayer player){
		EntityVehicleF_Physics vehicle = (EntityVehicleF_Physics) AEntityBase.createdEntities.get(vehicleID);
		if(vehicle != null && vehicle.definition != null){
			if(handle(world, player, vehicle) && !world.isClient()){
				InterfaceNetwork.sendToClientsTracking(this, vehicle);
			}
		}
	}
	
	/**
	 *  Helper method for handling clamped values.  Mainly comes from
	 *  control packets where we could go outside our desired bounds if we
	 *  don't check clamping.
	 */
	protected static int clampAngle(int min, int max, int value){
		return value < min ? min : (value > max ? max : value);
	}
	
	/**
	 *  Handler method with an extra parameter for the vehicle that this packet
	 *  is associated with. If the vehicle is null, or if it hasn't loaded it's JSON,
	 *  then this method won't be called.  Saves having to do null checks for every packet type.
	 *  If this is handled on the server, and a packet shouldn't be sent to all clients (like
	 *  if the action failed due to an issue) return false.  Otherwise, return true to 
	 *  send this packet on to all clients.  Return method has no function on clients.
	 */
	protected abstract boolean handle(WrapperWorld world, WrapperEntityPlayer player, EntityVehicleF_Physics vehicle);
}
