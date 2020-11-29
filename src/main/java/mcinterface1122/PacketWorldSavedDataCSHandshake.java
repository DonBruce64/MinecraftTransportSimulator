package mcinterface1122;

import io.netty.buffer.ByteBuf;
import mcinterface1122.WrapperWorld.InterfaceWorldSavedData;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketBase;

/**Packet used to request world NBT data from the server, and to send that data back to clients.
 * Used when world data is requested on a client, as MC is too dumb to let us simply set a flag to
 * get the NBT data from the server in an easy way.
 * 
 * @author don_bruce
 */
class PacketWorldSavedDataCSHandshake extends APacketBase{
	private final int worldID;
	private final IWrapperNBT data;
	
	public PacketWorldSavedDataCSHandshake(int worldID, IWrapperNBT data){
		super(null);
		this.worldID = worldID;
		this.data = data;
	}
	
	public PacketWorldSavedDataCSHandshake(ByteBuf buf){
		super(buf);
		this.worldID = buf.readInt();
		if(buf.readBoolean()){
			this.data = MasterInterface.networkInterface.createDataFromBuffer(buf);
		}else{
			this.data = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(worldID);
		if(data != null){
			buf.writeBoolean(true);
			data.writeToBuffer(buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public void handle(IWrapperWorld world, IWrapperPlayer player){
		if(world.isClient()){
			//Set the world saved data.
			((WrapperWorld) world).savedDataAccessor = new InterfaceWorldSavedData(WrapperWorld.dataID);
			((WrapperWorld) world).savedDataAccessor.readFromNBT(((WrapperNBT) data).tag);
		}else{
			//Send back a packet to the player who requested it.
			player.sendPacket(new PacketWorldSavedDataCSHandshake(world.getDimensionID(), world.getData()));
		}
	}
}
