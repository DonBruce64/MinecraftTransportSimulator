package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.ABuilderEntityBase;
import minecrafttransportsimulator.mcinterface.BuilderEntityExisting;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

/**Packet used to request NBT data for entities from the server, and to send that data back to clients.
 * Used when an entity is first created on a client, as MC is too dumb to let us simply set a flag to
 * get the NBT data from the server in an easy way.  Note that this packet takes an instance of
 * {@link BuilderEntityExisting}.  This is because the actual entity won't be present on the clients
 * until this packet it sent to them.  This prevents all the odd null checks that come from not having
 * the data on the client when the entity is first spawned.
 * 
 * @author don_bruce
 */
public class PacketEntityCSHandshake extends APacketBase{
	private final int builderID;
	private final WrapperNBT data;
	
	public PacketEntityCSHandshake(int builderID, WrapperNBT data){
		super(null);
		this.builderID = builderID;
		this.data = data;
	}
	
	public PacketEntityCSHandshake(ByteBuf buf){
		super(buf);
		this.builderID = buf.readInt();
		if(buf.readBoolean()){
			this.data = readDataFromBuffer(buf);
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
			writeDataToBuffer(data, buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperPlayer player){
		//Need to do null checks here as some entities may not exist due to them being unloaded during packet transfer.
		ABuilderEntityBase builder = (ABuilderEntityBase) world.world.getEntityByID(builderID);
		if(builder != null){
			if(world.isClient()){
				//Create the entity from loaded data.
				builder.handleLoadedNBT(data.tag);
			}else{
				//Queue up the builder to send the player data back next update.
				builder.playersRequestingData.add(player);
			}
		}
	}
}
