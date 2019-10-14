package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import minecrafttransportsimulator.vehicles.main.EntityVehicleA_Base;
import minecrafttransportsimulator.vehicles.parts.APart;
import net.minecraft.network.PacketBuffer;


/**Base packet used for vehicle part code.  This packet gets sent with
 * the vehicle date attached with the super of this class.  Note that 
 * although we give the part positions in the second constructor,
 * the part may not be actually present that that position.
 * This allows for packets to be sent that reference where
 * parts <i>could</i> be, like in spawning operations.
 * Because of this, the getPart() function may return null!
 * 
 * @author don_bruce
 */
public abstract class APacketVehiclePart extends APacketVehicle{
	private double x;
	private double y;
	private double z;

	public APacketVehiclePart(){}
	
	public APacketVehiclePart(EntityVehicleA_Base vehicle, double x, double y, double z){
		super(vehicle);
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public APacketVehiclePart(APart part){
		super(part.vehicle);
		this.x = part.offset.x;
		this.y = part.offset.y;
		this.z = part.offset.z;
	}
	
	@Override
	public void populateFromBytes(PacketBuffer buf){
		super.fromBytes(buf);
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
	}

	@Override
	public void convertToBytes(PacketBuffer buf){
		super.toBytes(buf);
		buf.writeDouble(this.x);
		buf.writeDouble(this.y);
		buf.writeDouble(this.z);
	}
	
	protected static APart getPart(APacketVehiclePart message, MTSWorldInterface world){
		EntityVehicleA_Base vehicle = getVehicle(message, world);
		//We may be null if this packet was delayed and we lost this vehicle on a client.
		if(vehicle != null){
			for(APart part : vehicle.getVehicleParts()){
				if(part.offset.x == message.x && part.offset.y == message.y && part.offset.z == message.z){
					return part;
				}
			}
		}
		return null;
	}
}
