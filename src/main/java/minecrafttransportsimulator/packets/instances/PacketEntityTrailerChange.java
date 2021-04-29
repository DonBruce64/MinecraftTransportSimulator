package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.EntityConnection;
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
	private final int entityID;
	private final int otherEntityID;
	private final int groupIndex;
	private final int connectionIndex;
	private final int otherGroupIndex;
	private final int otherConnectionIndex;
	private final boolean connect;
	
	public PacketEntityTrailerChange(AEntityD_Interactable<?> entityOn, EntityConnection towingConnection, boolean connect){
		super(entityOn);
		this.entityID = entityOn.lookupID;
		this.otherEntityID = towingConnection.otherEntity.lookupID;
		this.groupIndex = towingConnection.groupIndex;
		this.connectionIndex = towingConnection.connectionIndex;
		this.otherGroupIndex = towingConnection.otherGroupIndex;
		this.otherConnectionIndex = towingConnection.otherConnectionIndex;
		this.connect = connect;
	}
	
	public PacketEntityTrailerChange(ByteBuf buf){
		super(buf);
		this.entityID = buf.readInt();
		this.otherEntityID = buf.readInt();
		this.groupIndex = buf.readInt();
		this.connectionIndex = buf.readInt();
		this.otherGroupIndex = buf.readInt();
		this.otherConnectionIndex = buf.readInt();
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(entityID);
		buf.writeInt(otherEntityID);
		buf.writeInt(groupIndex);
		buf.writeInt(connectionIndex);
		buf.writeInt(otherGroupIndex);
		buf.writeInt(otherConnectionIndex);
		buf.writeBoolean(connect);
	}
	
	@Override
	public boolean handle(WrapperWorld world, AEntityD_Interactable<?> entity){
		EntityConnection connection = new EntityConnection(AEntityA_Base.getEntity(world, entityID), groupIndex, connectionIndex, AEntityA_Base.getEntity(world, otherEntityID), otherGroupIndex, otherConnectionIndex);
		if(connect){
			entity.connectTrailer(connection);
		}else{
			entity.disconnectTrailer(connection);
		}
		//Reset ground GUI if we changed a vehicle's trailer.
		if(InterfaceGUI.getActiveGUI() instanceof GUIPanelGround){
			GUIPanelGround panel = (GUIPanelGround) InterfaceGUI.getActiveGUI();
			if(panel.vehicle.equals(entity)){
				InterfaceGUI.closeGUI();
				InterfaceGUI.openGUI(new GUIPanelGround(panel.vehicle));
			}
		}
		return true;
	}
}
