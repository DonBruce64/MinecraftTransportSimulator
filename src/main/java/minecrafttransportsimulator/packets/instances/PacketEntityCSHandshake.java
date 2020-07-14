package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import mcinterface.BuilderEntity;
import mcinterface.WrapperEntityPlayer;
import mcinterface.WrapperNBT;
import mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

/**Packet used to request NBT data for entities from the server, and to send that data back to clients.
 * Used when an entity is first created on a client, as MC is too dumb to let us simply set a flag to
 * get the NBT data from the server in an easy way.  Note that this packet takes an instance of
 * {@link BuilderEntity}.  This is because the actual entity won't be present on the clients
 * until this packet it sent to them.  This prevents all the odd null checks that come from not having
 * the data on the client when the entity is first spawned.
 * 
 * @author don_bruce
 */
public class PacketEntityCSHandshake extends APacketBase{
	private final int ID;
	private final WrapperNBT data;
	
	public PacketEntityCSHandshake(int id, WrapperNBT data){
		super(null);
		this.data = data;
	}
	
	public PacketEntityCSHandshake(ByteBuf buf){
		super(buf);
		if(buf.readBoolean()){
			this.data = new WrapperNBT(buf);
		}else{
			this.data = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		if(data != null){
			buf.writeBoolean(true);
			data.writeToBuffer(buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperEntityPlayer player){
		if(world.isClient()){
			//Load the data into the entity.
			
		}else{
			//Send back a packet to the player who requested it.
			player.sendPacket(new PacketEntityCSHandshake(entity));
		}
	}
}
