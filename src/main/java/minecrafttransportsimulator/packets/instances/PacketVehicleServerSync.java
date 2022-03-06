package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used to send server vehicle movement to clients.  This packet doesn't directly
 * move the vehicle (such an action results in horrid movement lag due to network
 * latency and packet handling pipelines), rather it simply tells the client how much
 * the vehicle has moved since a last reference time since it was first spawned.  
 * This is slightly different than {@link PacketVehicleServerMovement}, as that packet
 * is purely a delta packet, where as this one is a snapshot in time.  These both are needed
 * because packets can arrive out of order, so snapshot packets can't be used for syncing, but
 * with delta packets a single missed packet can result in the entity being permanently de-synced.
 * Using both allows for a general increment, plus a "resetting" operation ever so often.
 * 
 * @author don_bruce
 */
public class PacketVehicleServerSync extends APacketEntity<EntityVehicleF_Physics>{
	private final Point3D motion;
	private final Point3D rotation;
	private final double pathing;
	private final long ticksExisted;
	
	public PacketVehicleServerSync(EntityVehicleF_Physics vehicle, Point3D motion, Point3D rotation, double pathing){
		super(vehicle);
		this.motion = motion;
		this.rotation = rotation;
		this.pathing = pathing;
		this.ticksExisted = vehicle.ticksExisted;
	}
	
	public PacketVehicleServerSync(ByteBuf buf){
		super(buf);
		this.motion = readPoint3dFromBuffer(buf);
		this.rotation = readPoint3dFromBuffer(buf);
		this.pathing = buf.readDouble();
		this.ticksExisted = buf.readLong();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(motion, buf);
		writePoint3dToBuffer(rotation, buf);
		buf.writeDouble(pathing);
		buf.writeLong(ticksExisted);
	}
	
	@Override
	public boolean handle(WrapperWorld world, EntityVehicleF_Physics vehicle){
		vehicle.syncServerDeltas(motion, rotation, pathing, ticksExisted);
		return false;
	}
}
