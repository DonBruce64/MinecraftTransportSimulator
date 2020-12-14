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
	private final int hitchIndex;
	private final int hookupIndex;
	
	public PacketVehicleTrailerChange(EntityVehicleF_Physics vehicle, int hitchIndex, int hookupIndex){
		super(vehicle);
		this.linkedID = vehicle.towedVehicle != null ? vehicle.towedVehicle.lookupID : -1;
		this.hitchIndex = hitchIndex;
		this.hookupIndex = hookupIndex;
	}
	
	public PacketVehicleTrailerChange(ByteBuf buf){
		super(buf);
		this.linkedID = buf.readInt();
		this.hitchIndex = buf.readInt();
		this.hookupIndex = buf.readInt();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(linkedID);
		buf.writeInt(hitchIndex);
		buf.writeInt(hookupIndex);
	}
	
	@Override
	public boolean handle(IWrapperWorld world, IWrapperPlayer player, EntityVehicleF_Physics vehicle){
		if(linkedID != -1){
			for(AEntityBase entity : AEntityBase.createdClientEntities){
				if(entity.lookupID == linkedID){
					vehicle.changeTrailer((EntityVehicleF_Physics) entity, hitchIndex, hookupIndex);
					break;
				}
			}
		}else{
			if(vehicle.towedVehicle != null){
				vehicle.changeTrailer(null, hitchIndex, hookupIndex);
			}
		}
		return true;
	}
}
