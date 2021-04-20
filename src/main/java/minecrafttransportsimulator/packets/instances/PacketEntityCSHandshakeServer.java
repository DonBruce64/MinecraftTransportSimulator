package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.ABuilderEntityBase;
import minecrafttransportsimulator.mcinterface.WrapperEntity;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

/**Packet used to send NBT data to clients when requested for it.  Driven by the arrival of a
 * {@link PacketEntityCSHandshakeClient} on the server.
 * 
 * @author don_bruce
 */
public class PacketEntityCSHandshakeServer extends APacketBase{
	private final String builderID;
	private final WrapperNBT data;
	
	public PacketEntityCSHandshakeServer(ABuilderEntityBase builder, WrapperNBT data){
		super(null);
		this.builderID = builder.getCachedUniqueIdString();
		this.data = data;
	}
	
	public PacketEntityCSHandshakeServer(ByteBuf buf){
		super(buf);
		this.builderID = readStringFromBuffer(buf);
		this.data = readDataFromBuffer(buf);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(builderID, buf);
		writeDataToBuffer(data, buf);
	}
	
	@Override
	public void handle(WrapperWorld world){
		//Need to do null checks here as some entities may not exist due to them being unloaded during packet transfer.
		WrapperEntity builderWrapper = world.getEntity(builderID);
		if(builderWrapper != null){
			//Create the entity from loaded data.
			((ABuilderEntityBase) builderWrapper.entity).handleLoadedNBT(data.tag);
		}
	}
}
