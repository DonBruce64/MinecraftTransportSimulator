package minecrafttransportsimulator.mcinterface;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.packets.components.APacketPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

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
	
	public PacketEntityCSHandshakeClient(WrapperPlayer player, BuilderTileEntity<?> builder){
		super(player);
		this.builderID = builder.getPos().getX() + "," + builder.getPos().getY() + "," + builder.getPos().getZ();
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
		//Queue up the builder to send the player data back next update.
		if(builderID.contains(",")){
			String[] stringPos = builderID.split(",");
			BuilderTileEntity<?> tile = (BuilderTileEntity<?>) world.world.getTileEntity(new BlockPos(Integer.valueOf(stringPos[0]), Integer.valueOf(stringPos[1]), Integer.valueOf(stringPos[2])));
			if(tile != null){
				tile.playersRequestingData.add(player);
			}
		}else{
			for(Entity entity : world.world.loadedEntityList){
				if(entity.getCachedUniqueIdString().equals(builderID)){
					//Queue up the builder to send the player data back next update.
					((ABuilderEntityBase) entity).playersRequestingData.add(player);
					break;
				}
			}
		}
	}
}
