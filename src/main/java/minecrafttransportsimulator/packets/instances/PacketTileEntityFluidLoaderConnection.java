package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityFluidLoader;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.entities.components.AEntityE_Multipart;
import minecrafttransportsimulator.entities.instances.PartInteractable;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**Packet sent to fluid loaders  on clients to change what part they are connected to.
 * 
 * @author don_bruce
 */
public class PacketTileEntityFluidLoaderConnection extends APacketEntity<TileEntityFluidLoader>{
	private final int linkedID;
	private final Point3d partOffset;
	private final boolean connect;
	
	public PacketTileEntityFluidLoaderConnection(TileEntityFluidLoader loader, boolean connect){
		super(loader);
		this.linkedID = loader.connectedPart.entityOn.lookupID;
		this.partOffset = loader.connectedPart.placementOffset;
		this.connect = connect;
	}
	
	public PacketTileEntityFluidLoaderConnection(ByteBuf buf){
		super(buf);
		this.linkedID = buf.readInt();
		this.partOffset = readPoint3dFromBuffer(buf);
		this.connect = buf.readBoolean();
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(linkedID);
		buf.writeBoolean(connect);
		writePoint3dToBuffer(partOffset, buf);
	}
	
	@Override
	protected boolean handle(WrapperWorld world, WrapperPlayer player, TileEntityFluidLoader loader){
		AEntityE_Multipart<?> entity = (AEntityE_Multipart<?>) AEntityA_Base.getEntity(world, linkedID);
		if(entity != null){
			if(connect){
				loader.connectedPart = (PartInteractable) entity.getPartAtLocation(partOffset);
				loader.getTank().resetAmountDispensed();
			}else{
				loader.connectedPart = null;
			}
		}
		return true;
	}
}
