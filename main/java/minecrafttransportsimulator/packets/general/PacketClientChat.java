package minecrafttransportsimulator.packets.general;

import minecrafttransportsimulator.mcinterface.MTSChatInterface;
import minecrafttransportsimulator.mcinterface.MTSNetwork.MTSPacket;
import minecrafttransportsimulator.mcinterface.MTSPlayerInterface;
import minecrafttransportsimulator.mcinterface.MTSWorldInterface;
import net.minecraft.nbt.NBTTagCompound;


/**Send this packet to any client that needs a message
 * displayed in their chat.  Will translate that message
 * using .lang files if it is possible.
 * 
 * @author don_bruce
 */
public class PacketClientChat extends MTSPacket{
	private String message;

	public PacketClientChat(){}
	
	public PacketClientChat(String message){
		this.message = message;
	}
	
	@Override
	public void parseFromNBT(NBTTagCompound tag){
		message = tag.getString("message");
	}

	@Override
	public void convertToNBT(NBTTagCompound tag){
		tag.setString("message", message);
	}
	
	@Override
	public void handlePacket(MTSWorldInterface world, boolean onServer){
		MTSChatInterface.displayChatMessage(message);
	}
}
