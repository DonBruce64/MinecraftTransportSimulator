package minecrafttransportsimulator.packets.multipart;

import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import minecrafttransportsimulator.systems.SFXSystem;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartClientRemoval extends APacketMultipart{

	public PacketMultipartClientRemoval(){}
	
	public PacketMultipartClientRemoval(EntityMultipartE_Vehicle vehicle){
		super(vehicle);
	}

	public static class Handler implements IMessageHandler<PacketMultipartClientRemoval, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartClientRemoval message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartE_Vehicle vehicle = (EntityMultipartE_Vehicle) getMultipart(message, ctx);
					if(vehicle != null){
						SFXSystem.stopMultipartSounds(vehicle);
					}
				}
			});
			return null;
		}
	}
}
