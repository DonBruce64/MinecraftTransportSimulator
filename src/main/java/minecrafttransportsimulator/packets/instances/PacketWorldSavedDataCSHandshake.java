package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**Packet used to request world NBT data from the server, and to send that data back to clients.
 * REquesting an empty string will return all data, whereas requesting a specific tag will
 * return that specific tag only.
 * 
 * @author don_bruce
 */
public class PacketWorldSavedDataCSHandshake extends APacketPlayer{
	private final String name;
	private final IWrapperNBT data;
	
	public PacketWorldSavedDataCSHandshake(IWrapperPlayer player, String name, IWrapperNBT data){
		super(player);
		this.name = name;
		this.data = data;
	}
	
	public PacketWorldSavedDataCSHandshake(ByteBuf buf){
		super(buf);
		this.name = readStringFromBuffer(buf);
		if(buf.readBoolean()){
			this.data = readDataFromBuffer(buf);
		}else{
			this.data = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(name, buf);
		if(data != null){
			buf.writeBoolean(true);
			writeDataToBuffer(data, buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public void handle(AWrapperWorld world, IWrapperPlayer player){
		if(world.isClient()){
			//Set the world saved data.
			world.setData(name, data);
		}else{
			//Send back a packet to the player who requested it.
			IWrapperNBT savedData = world.getData(name);
			if(name.isEmpty()){
				//Full data block, break up and send back in batches.
				for(String dataName : savedData.getAllNames()){
					player.sendPacket(new PacketWorldSavedDataCSHandshake(player, dataName, savedData.getData(dataName)));
				}
			}else{
				//Partial block, send as-is.
				player.sendPacket(new PacketWorldSavedDataCSHandshake(player, name, savedData));
			}
		}
	}
	
	@Override
	public boolean runOnMainThread(){
		return false;
	}
}
