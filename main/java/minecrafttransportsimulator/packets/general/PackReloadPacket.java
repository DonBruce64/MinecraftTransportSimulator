package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import minecrafttransportsimulator.multipart.parts.APart;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PackReloadPacket implements IMessage{

	public PackReloadPacket() {}
	
	@Override
	public void fromBytes(ByteBuf buf){}

	@Override
	public void toBytes(ByteBuf buf){}

	public static class Handler implements IMessageHandler<PackReloadPacket, IMessage>{
		public IMessage onMessage(final PackReloadPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					World world;
					if(ctx.side.isServer()){
						world = ctx.getServerHandler().playerEntity.worldObj;
					}else{
						world = Minecraft.getMinecraft().theWorld;
					}
					for(Entity entity : world.loadedEntityList){
						if(entity instanceof EntityMultipartA_Base){
							EntityMultipartA_Base multipart = (EntityMultipartA_Base) entity;
							if(multipart.pack != null){
								multipart.pack = PackParserSystem.getMultipartPack(multipart.multipartName);
							}
							for(APart part : multipart.getMultipartParts()){
								part.pack = PackParserSystem.getPartPack(part.partName);
							}
						}
					}
				}
			});
			if(ctx.side.isServer()){
				return message;
			}else{
				return null;
			}
		}
	}
}
