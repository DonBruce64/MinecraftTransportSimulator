package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SirenPacket implements IMessage{	
	private int id;

	public SirenPacket() { }
	
	public SirenPacket(int id){
		this.id=id;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
	}

	public static class Handler implements IMessageHandler<SirenPacket, IMessage>{
		public IMessage onMessage(final SirenPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartE_Vehicle thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityMultipartE_Vehicle) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					}else{
						thisEntity = (EntityMultipartE_Vehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						thisEntity.sirenOn = !thisEntity.sirenOn;
						
						if(ctx.side.isServer()){
							MTS.MTSNet.sendToAll(message);
						}
					}
				}
			});
			return null;
		}
	}
}