package minecraftflightsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class ChatPacket implements IMessage{
	private String chatMessage;	

	public ChatPacket() { }
	
	public ChatPacket(String chatMessage){
		this.chatMessage=chatMessage;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.chatMessage=ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		ByteBufUtils.writeUTF8String(buf, this.chatMessage);
	}

	public static class Handler implements IMessageHandler<ChatPacket, IMessage> {
		@Override
		public IMessage onMessage(ChatPacket message, MessageContext ctx) {
			if(ctx.side==Side.CLIENT){
				Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message.chatMessage));
			}
			return null;
		}
	}

}
