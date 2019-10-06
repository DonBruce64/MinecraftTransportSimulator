package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TrimPacket implements IMessage{	
	private int id;
	private byte trimCode;	

	public TrimPacket() { }
	
	public TrimPacket(int id, byte trimCode){
		this.id=id;
		this.trimCode=trimCode;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.trimCode=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.trimCode);
	}

	public static class Handler implements IMessageHandler<TrimPacket, IMessage>{
		public IMessage onMessage(final TrimPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleF_Air thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityVehicleF_Air) ctx.getServerHandler().player.world.getEntityByID(message.id);
					}else{
						thisEntity = (EntityVehicleF_Air) Minecraft.getMinecraft().world.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						if((message.trimCode & 3) == 0){
							if((message.trimCode >> 3) == 1){
								if(thisEntity.aileronTrim < 100){
									++thisEntity.aileronTrim;
								}
							}else{
								if(thisEntity.aileronTrim > -100){
									--thisEntity.aileronTrim;
								}
							}
						}else if((message.trimCode & 3) == 1){
							if((message.trimCode >> 3) == 1){
								if(thisEntity.elevatorTrim < 150){
									++thisEntity.elevatorTrim;
								}
							}else{
								if(thisEntity.elevatorTrim > -150){
									--thisEntity.elevatorTrim;
								}
							}
						}else if((message.trimCode & 3) == 2){
							if((message.trimCode >> 3) == 1){
								if(thisEntity.rudderTrim < 200){
									++thisEntity.rudderTrim;
								}
							}else{
								if(thisEntity.rudderTrim > -200){
									--thisEntity.rudderTrim;	
								}
							}
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