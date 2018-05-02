package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.multipart.parts.AMultipartPart;
import minecrafttransportsimulator.multipart.parts.PartGroundDevice;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PacketFlatGroundDevice extends APacketPart{
	public PacketFlatGroundDevice(){}
	
	public PacketFlatGroundDevice(AMultipartPart part){
		super(part);
	}

	public static class Handler implements IMessageHandler<PacketFlatGroundDevice, IMessage>{
		@Override
		public IMessage onMessage(final PacketFlatGroundDevice message, final MessageContext ctx){
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
