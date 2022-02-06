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
		if(loader.connectedPart != null){
			this.linkedID = loader.connectedPart.entityOn.uniqueUUID;
			this.partOffset = loader.connectedPart.placementOffset;
		}else{
			this.linkedID = null;
			this.partOffset = null;
		}
		this.connect = connect;
	}
	
	public PacketTileEntityLoaderConnection(ByteBuf buf){
		super(buf);
		if(buf.readBoolean()){
			this.linkedID = readUUIDFromBuffer(buf);
			this.partOffset = readPoint3dFromBuffer(buf);
		}else{
			this.linkedID = null;
			this.partOffset = null;
		}
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		if(linkedID != null){
			buf.writeBoolean(true);
			writeUUIDToBuffer(linkedID, buf);
			writePoint3dToBuffer(partOffset, buf);
		}else{
			buf.writeBoolean(false);
		}
		buf.writeBoolean(connect);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, ATileEntityLoader loader){
		AEntityF_Multipart<?> entity = world.getEntity(linkedID);
		if(connect){
			if(entity != null){
				loader.connectToPart((PartInteractable) entity.getPartAtLocation(partOffset));
			}
		}else{
			loader.connectToPart(null);
		}
		return true;
	}
}
