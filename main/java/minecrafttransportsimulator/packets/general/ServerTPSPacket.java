package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ServerTPSPacket implements IMessage{
	private int dimID;

	public ServerTPSPacket() { }
	
	public ServerTPSPacket(int dimID){
		this.dimID = dimID;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.dimID=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.dimID);
	}

	public static class Handler implements IMessageHandler<ServerTPSPacket, IMessage>{
		@Override
		public IMessage onMessage(final ServerTPSPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					if(Minecraft.getMinecraft().theWorld.provider.getDimension() == message.dimID){
						for(Entity entity : Minecraft.getMinecraft().theWorld.loadedEntityList){
							if(entity instanceof EntityMultipartParent){
								++((EntityMultipartParent) entity).clientTicksToSkip;
							}
						}
					}
				}
			});
			return null;
		}
	}
}
