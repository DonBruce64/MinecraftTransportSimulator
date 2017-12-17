package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PackPacket implements IMessage{

	public PackPacket() {}
	
	@Override
	public void fromBytes(ByteBuf buf){}

	@Override
	public void toBytes(ByteBuf buf){}

	public static class Handler implements IMessageHandler<PackPacket, IMessage>{
		public IMessage onMessage(final PackPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					PackParserSystem.init();
					World world;
					if(ctx.side.isServer()){
						world = ctx.getServerHandler().playerEntity.worldObj;
					}else{
						world = Minecraft.getMinecraft().theWorld;
					}
					for(Entity entity : world.loadedEntityList){
						if(entity instanceof EntityMultipartMoving){
							EntityMultipartMoving mover = (EntityMultipartMoving) entity;
							if(!mover.name.isEmpty()){
								mover.pack = PackParserSystem.getPack(mover.name);
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
