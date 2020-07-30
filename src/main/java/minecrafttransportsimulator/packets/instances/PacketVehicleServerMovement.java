package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.WrapperPlayer;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;

/**Packet used to send server vehicle movement to clients.  This packet doesn't directly
 * move the vehicle (such an action results in horrid movement lag due to network
 * latency and packet handling pipelines), rather it simply tells the client how much
 * the vehicle has moved since it was first spawned.  This way clients can adjust the
 * vehicle position while at the same time running their own calculations to "soft-sync"
 * the position and rotation.  This system of syncing has the side-effect of significant
 * rubberbanding when server TPS suffers or networking goes bad, but it's far better than
 * the alternatives when the connection is good, hence why we use it.
 * 
 * @author don_bruce
 */
public class PacketVehicleServerMovement extends APacketVehicle{
	private final Point3d motion;
	private final Point3d rotation;
	
	public PacketVehicleServerMovement(EntityVehicleF_Physics vehicle, Point3d motion, Point3d rotation){
		super(vehicle);
		this.motion = motion;
		this.rotation = rotation;
	}
	
	public PacketVehicleServerMovement(ByteBuf buf){
		super(buf);
		this.motion = readPoint3dFromBuffer(buf);
		this.rotation = readPoint3dFromBuffer(buf);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writePoint3dToBuffer(motion, buf);
		writePoint3dToBuffer(rotation, buf);
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		vehicle.addToServerDeltas(motion, rotation);
		return false;
	}
}
