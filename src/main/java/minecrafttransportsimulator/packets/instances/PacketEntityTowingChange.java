package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.TowingConnection;
import minecrafttransportsimulator.entities.components.AEntityG_Towable;
import minecrafttransportsimulator.guis.components.AGUIBase;
import minecrafttransportsimulator.guis.instances.GUIPanelGround;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet used to send signals to entities to connect/disconnect towing entities.  Sent from the server to all clients when 
 * towing linking changes.
 * 
 * @author don_bruce
 */
public class PacketEntityTowingChange extends APacketEntity<AEntityG_Towable<?>>{
	private final WrapperNBT connectionData;
	private final boolean connect;
	
	public PacketEntityTowingChange(TowingConnection connection, boolean connect){
		super(connection.towingVehicle);
		this.connectionData = connection.save(InterfaceCore.getNewNBTWrapper());
		this.connect = connect;
	}
	
	public PacketEntityTowingChange(ByteBuf buf){
		super(buf);
		this.connectionData = readDataFromBuffer(buf);
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeDataToBuffer(connectionData, buf);
		buf.writeBoolean(connect);
	}
	
	@Override
	public boolean handle(WrapperWorld world, AEntityG_Towable<?> hitchEntity){
		TowingConnection connection = new TowingConnection(connectionData);
		if(connection.initConnection(world)){
			if(connect){
				hitchEntity.connectTrailer(connection);
			}else{
				hitchEntity.disconnectTrailer(connection);
			}
			if(AGUIBase.activeInputGUI instanceof GUIPanelGround){
				((GUIPanelGround) AGUIBase.activeInputGUI).handleConnectionChange(connection);
			}
			return true;
		}else{
			return false;
		}
	}
}
