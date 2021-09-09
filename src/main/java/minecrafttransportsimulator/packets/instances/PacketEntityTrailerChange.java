package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityD_Interactable;
import minecrafttransportsimulator.guis.components.InterfaceGUI;
import minecrafttransportsimulator.guis.instances.GUIPanelGround;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used to send signals to entities to connect/disconnect trailers.  Sent from the server to all clients when trailer
 * linking changes.
 * 
 * @author don_bruce
 */
public class PacketEntityTrailerChange extends APacketEntity<AEntityD_Interactable<?>>{
	private final int hookupEntityID;
	private final int hitchGroupIndex;
	private final int hitchConnectionIndex;
	private final int hookupGroupIndex;
	private final int hookupConnectionIndex;
	private final boolean connect;
	
	public PacketEntityTrailerChange(TrailerConnection connection, boolean connect){
		super(connection.hitchEntity);
		this.hookupEntityID = connection.hookupEntity.lookupID;
		this.hitchGroupIndex = connection.hitchGroupIndex;
		this.hitchConnectionIndex = connection.hitchConnectionIndex;
		this.hookupGroupIndex = connection.hookupGroupIndex;
		this.hookupConnectionIndex = connection.hookupConnectionIndex;
		this.connect = connect;
	}
	
	public PacketEntityTrailerChange(ByteBuf buf){
		super(buf);
		this.hookupEntityID = buf.readInt();
		this.hitchGroupIndex = buf.readInt();
		this.hitchConnectionIndex = buf.readInt();
		this.hookupGroupIndex = buf.readInt();
		this.hookupConnectionIndex = buf.readInt();
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(hookupEntityID);
		buf.writeInt(hitchGroupIndex);
		buf.writeInt(hitchConnectionIndex);
		buf.writeInt(hookupGroupIndex);
		buf.writeInt(hookupConnectionIndex);
		buf.writeBoolean(connect);
	}
	
	@Override
	public boolean handle(WrapperWorld world, AEntityD_Interactable<?> hitchEntity){
		AEntityD_Interactable<?> hookupEntity = AEntityA_Base.getEntity(world, hookupEntityID);
		TrailerConnection connection = new TrailerConnection(hitchEntity, hitchGroupIndex, hitchConnectionIndex, hookupEntity, hookupGroupIndex, hookupConnectionIndex);
		if(connect){
			hitchEntity.connectTrailer(connection);
		}else{
			hitchEntity.disconnectTrailer(connection);
		}
		//Reset ground GUI if we changed a vehicle's trailer.
		if(InterfaceGUI.getActiveGUI() instanceof GUIPanelGround){
			((GUIPanelGround) InterfaceGUI.getActiveGUI()).handleConnectionChange(hitchEntity, hookupEntity);
		}
		return true;
	}
}
