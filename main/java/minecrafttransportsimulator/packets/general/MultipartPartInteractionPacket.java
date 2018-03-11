package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.core.EntityMultipartChild;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MultipartPartInteractionPacket implements IMessage{
	private int id;
	private int player;

	public MultipartPartInteractionPacket() {}
	
	public MultipartPartInteractionPacket(int id, int player){
		this.id = id;
		this.player = player;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.player=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeInt(this.player);
	}

	public static class Handler implements IMessageHandler<MultipartPartInteractionPacket, IMessage>{
		@Override
		public IMessage onMessage(final MultipartPartInteractionPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartChild child = (EntityMultipartChild) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					EntityPlayer player = (EntityPlayer) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.player);
					
					if(child != null && player != null){
						child.interactPart(player);
					}
				}
			});
			return null;
		}
	}
}
