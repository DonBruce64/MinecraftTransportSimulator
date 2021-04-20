package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.ABuilderEntityBase;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**Packet used to request NBT data for entities from the server.  Used when an entity is first created 
 * on a client, as MC is too dumb to let us simply set a flag to get the NBT data from the server in an 
 * easy way.  Once on the server, it will send back a {@link PacketEntityCSHandshakeServer}.
 * 
 * @author don_bruce
 */
public class PacketEntityCSHandshakeClient extends APacketPlayer{
	private final String builderID;
	
	public PacketEntityCSHandshakeClient(WrapperPlayer player, ABuilderEntityBase builder){
		super(player);
		this.builderID = builder.getCachedUniqueIdString();
	}
	
	public PacketEntityCSHandshakeClient(ByteBuf buf){
		super(buf);
		this.builderID = readStringFromBuffer(buf);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(builderID, buf);
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperPlayer player){
		//Need to do null checks here as some entities may not exist due to them being unloaded during packet transfer.
		WrapperEntity builderWrapper = world.getEntity(builderID);
		if(builderWrapper != null){
			//Queue up the builder to send the player data back next update.
			((ABuilderEntityBase) builderWrapper.entity).playersRequestingData.add(player);
		}
	}
}
