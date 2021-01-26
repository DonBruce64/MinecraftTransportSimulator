package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.mcinterface.WrapperWorld.InterfaceWorldSavedData;
import minecrafttransportsimulator.packets.components.APacketBase;

/**Packet used to request world NBT data from the server, and to send that data back to clients.
 * Used when world data is requested on a client, as MC is too dumb to let us simply set a flag to
 * get the NBT data from the server in an easy way.
 * 
 * @author don_bruce
 */
public class PacketWorldSavedDataCSHandshake extends APacketBase{
	private final WrapperNBT data;
	
	public PacketWorldSavedDataCSHandshake(WrapperNBT data){
		super(null);
		this.data = data;
	}
	
	public PacketWorldSavedDataCSHandshake(ByteBuf buf){
		super(buf);
		if(buf.readBoolean()){
			this.data = readDataFromBuffer(buf);
		}else{
			this.data = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		if(data != null){
			buf.writeBoolean(true);
			writeDataToBuffer(data, buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperPlayer player){
		if(world.isClient()){
			//Set the world saved data.
			world.savedDataAccessor = new InterfaceWorldSavedData(WrapperWorld.STORED_WORLD_DATA_ID);
			world.savedDataAccessor.readFromNBT(data.tag);
		}else{
			//Send back a packet to the player who requested it.
			WrapperNBT savedData = new WrapperNBT();
			world.savedDataAccessor.writeToNBT(savedData.tag);
			player.sendPacket(new PacketWorldSavedDataCSHandshake(savedData));
		}
	}
}
