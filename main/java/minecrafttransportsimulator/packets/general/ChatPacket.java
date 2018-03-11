package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ChatPacket implements IMessage{
	private String translatableMessage;

	public ChatPacket() { }
	
	public ChatPacket(String translatableMessage){
		this.translatableMessage=translatableMessage;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.translatableMessage=ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		ByteBufUtils.writeUTF8String(buf, this.translatableMessage);
	}

	public static class Handler implements IMessageHandler<ChatPacket, IMessage>{
		@Override
		public IMessage onMessage(final ChatPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentString(I18n.format(message.translatableMessage)));
				}
			});
			return null;
		}
	}

}
