package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSNetwork.MTSPacket;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import net.minecraft.network.PacketBuffer;


/**Base packet for vehicle interaction.  Contains the entity ID of the vehicle,
 * and a helper method for getting it from the world.
 * 
 * @author don_bruce
 */
public abstract class APacketVehicle extends MTSPacket{
	private int id;

	public APacketVehicle(){}
	
	public APacketVehicle(EntityVehicleA_Base vehicle){
		this.id = vehicle.getEntityId();
	}
	
	@Override
	public void populateFromBytes(PacketBuffer buf){
		this.id = buf.readInt();
	}

	@Override
	public void convertToBytes(PacketBuffer buf){
		buf.writeInt(this.id);
	}
	
	protected static EntityVehicleA_Base getVehicle(APacketVehicle packet, MTSWorldInterface world){
		return (EntityVehicleA_Base) world.getEntity(packet.id);
	}
}
