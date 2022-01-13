package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.entities.components.AEntityE_Interactable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPanelGround;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used to send signals to entities to connect/disconnect trailers.  Sent from the server to all clients when trailer
 * linking changes.
 * 
 * @author don_bruce
 */
public class PacketEntityTrailerChange extends APacketEntity<AEntityE_Interactable<?>>{
	private final UUID hookupEntityID;
	private final int hitchGroupIndex;
	private final int hitchConnectionIndex;
	private final int hookupGroupIndex;
	private final int hookupConnectionIndex;
	private final boolean connect;
	
	public PacketEntityTrailerChange(TrailerConnection connection, boolean connect){
		super(connection.hitchEntity);
		this.hookupEntityID = connection.hookupEntity.uniqueUUID;
		this.hitchGroupIndex = connection.hitchGroupIndex;
		this.hitchConnectionIndex = connection.hitchConnectionIndex;
		this.hookupGroupIndex = connection.hookupGroupIndex;
		this.hookupConnectionIndex = connection.hookupConnectionIndex;
		this.connect = connect;
	}
	
	public PacketEntityTrailerChange(ByteBuf buf){
		super(buf);
		this.hookupEntityID = readUUIDFromBuffer(buf);
		this.hitchGroupIndex = buf.readInt();
		this.hitchConnectionIndex = buf.readInt();
		this.hookupGroupIndex = buf.readInt();
		this.hookupConnectionIndex = buf.readInt();
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeUUIDToBuffer(hookupEntityID, buf);
		buf.writeInt(hitchGroupIndex);
		buf.writeInt(hitchConnectionIndex);
		buf.writeInt(hookupGroupIndex);
		buf.writeInt(hookupConnectionIndex);
		buf.writeBoolean(connect);
	}
	
	@Override
	public boolean handle(WrapperWorld world, AEntityE_Interactable<?> hitchEntity){
		AEntityE_Interactable<?> hookupEntity = world.getEntity(hookupEntityID);
		TrailerConnection connection = new TrailerConnection(hitchEntity, hitchGroupIndex, hitchConnectionIndex, hookupEntity, hookupGroupIndex, hookupConnectionIndex);
		if(connect){
			hitchEntity.connectTrailer(connection);
		}else{
			hitchEntity.disconnectTrailer(connection);
		}
		if(AGUIBase.activeInputGUI instanceof GUIPanelGround){
			((GUIPanelGround) AGUIBase.activeInputGUI).handleConnectionChange(hitchEntity, hookupEntity);
		}
		return true;
	}
}
