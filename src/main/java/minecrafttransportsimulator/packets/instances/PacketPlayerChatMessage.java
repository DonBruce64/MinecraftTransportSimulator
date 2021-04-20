package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketPlayer;

/**Packet used for sending the player chat messages from the server.  Mainly for informing them
 * about things they did to a vehicle they interacted with.  Do NOT send this packet to the server
 * or it will crash when it tries to display chat messages on something without a screen!
 * 
 * @author don_bruce
 */
public class PacketPlayerChatMessage extends APacketPlayer{
	private final String chatMessage;
	
	public PacketPlayerChatMessage(WrapperPlayer player, String chatMessage){
		super(player);
		this.chatMessage = chatMessage;
	}
	
	public PacketPlayerChatMessage(ByteBuf buf){
		super(buf);
		this.chatMessage = readStringFromBuffer(buf);
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		writeStringToBuffer(chatMessage, buf);
	}
	
	@Override
	public void handle(WrapperWorld world, WrapperPlayer player){
		player.displayChatMessage(chatMessage);
	}
}
