package minecrafttransportsimulator.packets.multipart;

import minecrafttransportsimulator.multipart.main.EntityMultipartB_Existing;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartWindowBreak extends APacketMultipart{

	public PacketMultipartWindowBreak(){}
	
	public PacketMultipartWindowBreak(EntityMultipartB_Existing multipart){
		super(multipart);
	}

	public static class Handler implements IMessageHandler<PacketMultipartWindowBreak, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartWindowBreak message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartB_Existing multipart = (EntityMultipartB_Existing) getMultipart(message, ctx);
					if(multipart != null){
						++multipart.brokenWindows;
					}	
				}
			});
			return null;
		}
	}

}
