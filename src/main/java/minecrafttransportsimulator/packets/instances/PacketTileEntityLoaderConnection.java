package minecrafttransportsimulator.packets.instances;

import java.util.UUID;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityLoader;
import minecrafttransportsimulator.entities.components.AEntityF_Multipart;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet sent to loaders  on clients to change what part they are connected to.
 * 
 * @author don_bruce
 */
public class PacketTileEntityLoaderConnection extends APacketEntity<ATileEntityLoader>{
	private final UUID linkedID;
	private final Point3d partOffset;
	private final boolean connect;
	
	public PacketTileEntityLoaderConnection(ATileEntityLoader loader, boolean connect){
		super(loader);
		this.linkedID = loader.connectedPart.entityOn.uniqueUUID;
		this.partOffset = loader.connectedPart.placementOffset;
		this.connect = connect;
	}
	
	public PacketTileEntityLoaderConnection(ByteBuf buf){
		super(buf);
		this.linkedID = readUUIDFromBuffer(buf);
		this.partOffset = readPoint3dFromBuffer(buf);
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeUUIDToBuffer(linkedID, buf);
		buf.writeBoolean(connect);
		writePoint3dToBuffer(partOffset, buf);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, ATileEntityLoader loader){
		AEntityF_Multipart<?> entity = world.getEntity(linkedID);
		if(entity != null){
			if(connect){
				loader.connectToPart((PartInteractable) entity.getPartAtLocation(partOffset));
			}else{
				loader.connectToPart(null);
			}
		}
		return true;
	}
}
