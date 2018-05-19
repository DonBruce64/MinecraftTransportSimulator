package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.multipart.parts.PartGroundDevice;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PacketPartGroundDeviceFlat extends APacketPart{
	public PacketPartGroundDeviceFlat(){}
	
	public PacketPartGroundDeviceFlat(APart part){
		super(part);
	}

	public static class Handler implements IMessageHandler<PacketPartGroundDeviceFlat, IMessage>{
		@Override
		public IMessage onMessage(final PacketPartGroundDeviceFlat message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					PartGroundDevice grounder = (PartGroundDevice) getMultipartPartFromMessage(message, ctx);
					if(grounder != null){
						grounder.setFlat();
					}
				}
			});
			return null;
		}
	}

}
