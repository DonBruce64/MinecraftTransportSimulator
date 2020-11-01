package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet used to send signals to vehicles to connect/disconnect trailers.  Sent from the server to all clients when trailer
 * linking changes.
 * 
 * @author don_bruce
 */
public class PacketVehicleTrailerChange extends APacketVehicle{
	private final int linkedID;
	
	public PacketVehicleTrailerChange(EntityVehicleF_Physics vehicle){
		super(vehicle);
		this.linkedID = vehicle.towedVehicle != null ? vehicle.towedVehicle.lookupID : -1;
	}
	
	public PacketVehicleTrailerChange(ByteBuf buf){
		super(buf);
		this.linkedID = buf.readInt();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(linkedID);
	}
	
	@Override
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle){
		if(linkedID != -1){
			for(AEntityBase entity : AEntityBase.createdClientEntities){
				if(entity.lookupID == linkedID){
					vehicle.towedVehicle = (EntityVehicleF_Physics) entity;
					((EntityVehicleF_Physics) entity).towedByVehicle = vehicle;
					vehicle.towedVehicle.parkingBrakeOn = false;
					break;
				}
			}
		}else{
			if(vehicle.towedVehicle != null){
				vehicle.towedVehicle.towedByVehicle = null;
				vehicle.towedVehicle.parkingBrakeOn = true;
				vehicle.towedVehicle = null;
			}
		}
		return true;
	}
}
