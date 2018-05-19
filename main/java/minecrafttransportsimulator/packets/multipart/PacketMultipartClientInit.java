package minecrafttransportsimulator.packets.multipart;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartClientInit extends APacketMultipart{

	public PacketMultipartClientInit(){}
	
	public PacketMultipartClientInit(EntityMultipartA_Base multipart){
		super(multipart);
	}

	public static class Handler implements IMessageHandler<PacketMultipartClientInit, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartClientInit message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartA_Base multipart = getMultipart(message, ctx);
					if(multipart!=null){
						NBTTagCompound tagCompound = new NBTTagCompound();
						multipart.writeToNBT(tagCompound);
						MTS.MTSNet.sendTo(new PacketMultipartClientInitResponse(multipart, tagCompound), ctx.getServerHandler().playerEntity);
					}
				}
			});
			return null;
		}
	}
}
