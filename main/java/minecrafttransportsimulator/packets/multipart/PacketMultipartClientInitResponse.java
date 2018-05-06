package minecrafttransportsimulator.packets.multipart;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartClientInitResponse extends APacketMultipart{
	private NBTTagCompound tagCompound;

	public PacketMultipartClientInitResponse(){}
	
	public PacketMultipartClientInitResponse(EntityMultipartA_Base multipart, NBTTagCompound tagCompound){
		super(multipart);
		this.tagCompound=tagCompound;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.tagCompound=ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeTag(buf, this.tagCompound);
	}

	public static class Handler implements IMessageHandler<PacketMultipartClientInitResponse, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartClientInitResponse message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartA_Base multipart = getMultipartFromMessage(message, ctx);
					if(multipart != null){
						multipart.readFromNBT(message.tagCompound);
					}
				}
			});
			return null;
		}
	}
}
