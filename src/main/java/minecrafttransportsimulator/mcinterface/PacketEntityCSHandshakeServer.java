package minecrafttransportsimulator.mcinterface;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

/**Packet used to send NBT data to clients when requested for it.  Driven by the arrival of a
 * {@link PacketEntityCSHandshakeClient} on the server.  This is used for both normal and tile
 * entities, depending on the format of the string passed-in.
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
	
	public PacketEntityCSHandshakeServer(BuilderTileEntity<?> builder, WrapperNBT data){
		super(null);
		this.builderID = builder.getPos().getX() + "," + builder.getPos().getY() + "," + builder.getPos().getZ();
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
		if(builderID.contains(",")){
			String[] stringPos = builderID.split(",");
			BuilderTileEntity<?> tile = (BuilderTileEntity<?>) world.world.getTileEntity(new BlockPos(Integer.valueOf(stringPos[0]), Integer.valueOf(stringPos[1]), Integer.valueOf(stringPos[2])));
			if(tile != null){
				tile.lastLoadedNBT = data.tag;
				tile.loadFromSavedNBT = true;
			}
		}else{
			for(Entity entity : world.world.loadedEntityList){
				if(entity.getCachedUniqueIdString().equals(builderID)){
					//Set last loaded NBT.
					((ABuilderEntityBase) entity).lastLoadedNBT = data.tag;
					((ABuilderEntityBase) entity).loadFromSavedNBT = true;
					break;
				}
			}
		}
	}
}
