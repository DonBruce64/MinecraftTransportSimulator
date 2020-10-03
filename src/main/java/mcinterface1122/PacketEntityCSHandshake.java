package mcinterface1122;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

/**Packet used to request NBT data for entities from the server, and to send that data back to clients.
 * Used when an entity is first created on a client, as MC is too dumb to let us simply set a flag to
 * get the NBT data from the server in an easy way.  Note that this packet takes an instance of
 * {@link BuilderEntity}.  This is because the actual entity won't be present on the clients
 * until this packet it sent to them.  This prevents all the odd null checks that come from not having
 * the data on the client when the entity is first spawned.
 * 
 * @author don_bruce
 */
class PacketEntityCSHandshake extends APacketBase{
	private final int builderID;
	private final IWrapperNBT data;
	
	public PacketEntityCSHandshake(int builderID, IWrapperNBT data){
		super(null);
		this.builderID = builderID;
		this.data = data;
	}
	
	public PacketEntityCSHandshake(ByteBuf buf){
		super(buf);
		this.builderID = buf.readInt();
		if(buf.readBoolean()){
			this.data = MasterInterface.networkInterface.createDataFromBuffer(buf);
		}else{
			this.data = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(builderID);
		if(data != null){
			buf.writeBoolean(true);
			data.writeToBuffer(buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public void handle(IWrapperWorld world, IWrapperPlayer player){
		if(world.isClient()){
			//Load the data into the entity.
			world.getEntity(builderID).setNBT(data);
		}else{
			//Send back a packet to the player who requested it.
			player.sendPacket(new PacketEntityCSHandshake(builderID, world.getEntity(builderID).getNBT()));
		}
	}
}
