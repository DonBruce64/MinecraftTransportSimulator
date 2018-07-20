package minecrafttransportsimulator.packets.multipart;

import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import minecrafttransportsimulator.multipart.parts.APart;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartClientPartRemoval extends APacketMultipartPart{

	public PacketMultipartClientPartRemoval(){}
	
	public PacketMultipartClientPartRemoval(EntityMultipartA_Base multipart, double offsetX, double offsetY, double offsetZ){
		super(multipart, offsetX, offsetY, offsetZ);
	}

	public static class Handler implements IMessageHandler<PacketMultipartClientPartRemoval, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartClientPartRemoval message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartA_Base multipart = getMultipart(message, ctx);
					APart partToRemove = multipart.getPartAtLocation(message.offsetX, message.offsetY, message.offsetZ);
					multipart.removePart(partToRemove, false);
				}
			});
			return null;
		}
	}

}
