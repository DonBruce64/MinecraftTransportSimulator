package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPackReload implements IMessage{
	public PacketPackReload(){}
	
	@Override
	public void fromBytes(ByteBuf buf){}

	@Override
	public void toBytes(ByteBuf buf){}

	public static class Handler implements IMessageHandler<PacketPackReload, IMessage>{
		@Override
		public IMessage onMessage(final PacketPackReload message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					PackParserSystem.reloadPackData();
					for(Entity entity : ctx.getServerHandler().playerEntity.worldObj.loadedEntityList){
						if(entity instanceof EntityMultipartA_Base){
							EntityMultipartA_Base multipart = (EntityMultipartA_Base) entity;
							multipart.pack = PackParserSystem.getMultipartPack(multipart.multipartName);
						}
					}
				}
			});
			return null;
		}
	}

}
