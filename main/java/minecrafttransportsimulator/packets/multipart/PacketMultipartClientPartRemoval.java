package minecrafttransportsimulator.packets.multipart;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.dataclasses.PackMultipartObject.PackPart;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import minecrafttransportsimulator.multipart.parts.APart;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketMultipartClientPartRemoval extends APacketMultipart{
	private byte partIndex;

	public PacketMultipartClientPartRemoval(){}
	
	public PacketMultipartClientPartRemoval(EntityMultipartA_Base multipart, byte partIndex){
		super(multipart);
		this.partIndex = partIndex;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.partIndex=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		buf.writeByte(this.partIndex);
	}

	public static class Handler implements IMessageHandler<PacketMultipartClientPartRemoval, IMessage>{
		@Override
		public IMessage onMessage(final PacketMultipartClientPartRemoval message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartA_Base multipart = (EntityMultipartA_Base) getMultipartFromMessage(message, ctx);
					PackPart packPart = multipart.pack.parts.get(message.partIndex);
					Vec3d partOffset = new Vec3d(packPart.pos[0], packPart.pos[1], packPart.pos[2]);
					for(APart part : multipart.getMultipartParts()){
						if(part.offset.equals(partOffset)){
							multipart.removePart(part, false);
						}
					}
				}
			});
			return null;
		}
	}

}
