package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.multipart.main.EntityMultipartE_Vehicle;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ThrottlePacket implements IMessage{
	private int id;
	private byte throttle;

	public ThrottlePacket(){}
	
	public ThrottlePacket(int id, byte throttle){
		this.id=id;
		this.throttle=throttle;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.throttle=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.throttle);
	}

	public static class Handler implements IMessageHandler<ThrottlePacket, IMessage>{
		public IMessage onMessage(final ThrottlePacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartE_Vehicle vehicle;
					if(ctx.side.isServer()){
						vehicle = (EntityMultipartE_Vehicle) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					}else{
						if(Minecraft.getMinecraft().theWorld == null){
							return;
						}
						vehicle = (EntityMultipartE_Vehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
						
					}
					if(vehicle!=null){
						if(message.throttle == Byte.MAX_VALUE){
							if(vehicle.throttle < 100){
								++vehicle.throttle;
							}
						}else if(message.throttle == Byte.MIN_VALUE){
							if(vehicle.throttle > 0){
								--vehicle.throttle;
							}
						}else{
							vehicle.throttle = message.throttle;
						}
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
