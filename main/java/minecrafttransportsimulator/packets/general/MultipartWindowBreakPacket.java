package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.core.EntityMultipartB_Existing;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MultipartWindowBreakPacket implements IMessage{
	private int id;

	public MultipartWindowBreakPacket() {}
	
	public MultipartWindowBreakPacket(int id){
		this.id = id;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
	}

	public static class Handler implements IMessageHandler<MultipartWindowBreakPacket, IMessage>{
		@Override
		public IMessage onMessage(final MultipartWindowBreakPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartB_Existing multipart = (EntityMultipartB_Existing) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					if(multipart != null){
						++multipart.brokenWindows;
					}	
				}
			});
			return null;
		}
	}

}
