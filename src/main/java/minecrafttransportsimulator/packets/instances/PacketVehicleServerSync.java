package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
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
	
	public PacketVehicleServerSync(EntityVehicleF_Physics vehicle){
		super(vehicle);
		this.motion = null;
		this.rotation = null;
		this.pathing = 0;
	}
	
	public PacketVehicleServerSync(EntityVehicleF_Physics vehicle, Point3D motion, Point3D rotation, double pathing){
		super(vehicle);
		this.motion = motion;
		this.rotation = rotation;
		this.pathing = pathing;
	}
	
	public PacketVehicleServerSync(ByteBuf buf){
		super(buf);
		if(buf.readBoolean()){
			this.motion = readPoint3dFromBuffer(buf);
			this.rotation = readPoint3dFromBuffer(buf);
			this.pathing = buf.readDouble();
		}else{
			this.motion = null;
			this.rotation = null;
			this.pathing = 0;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		if(motion != null){
			buf.writeBoolean(true);
			writePoint3dToBuffer(motion, buf);
			writePoint3dToBuffer(rotation, buf);
			buf.writeDouble(pathing);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public boolean handle(AWrapperWorld world, EntityVehicleF_Physics vehicle){
		vehicle.syncServerDeltas(motion, rotation, pathing);
		return false;
	}
}
