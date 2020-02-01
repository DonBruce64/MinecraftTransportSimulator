package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RudderPacket implements IMessage{
	private int id;
	private short value;
	private byte cooldown;
	

	public RudderPacket() { }
	
	public RudderPacket(int id, short value, byte cooldown){
		this.id = id;
		this.value = value;
		this.cooldown = cooldown;
	}
	
	public RudderPacket(int id, short rudderAngle){
		this.id = id;
		this.value = rudderAngle;
		this.cooldown = -1;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.value=buf.readShort();
		this.cooldown=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeShort(this.value);
		buf.writeByte(this.cooldown);
	}

	public static class Handler implements IMessageHandler<RudderPacket, IMessage>{
		public IMessage onMessage(final RudderPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleF_Air vehicle;
					if(ctx.side.isServer()){
						vehicle = (EntityVehicleF_Air) ctx.getServerHandler().player.world.getEntityByID(message.id);
					}else{
						if(Minecraft.getMinecraft().world == null){
							return;
						}
						vehicle = (EntityVehicleF_Air) Minecraft.getMinecraft().world.getEntityByID(message.id);
					}
					if(vehicle!=null){
						if(message.cooldown != -1){
							vehicle.rudderCooldown = message.cooldown;
							if(vehicle.rudderAngle + message.value > -vehicle.MAX_RUDDER_ANGLE && vehicle.rudderAngle + message.value < vehicle.MAX_RUDDER_ANGLE){
								vehicle.rudderAngle += message.value; 
							}else{
								return;
							}
						}else{
							vehicle.rudderAngle = message.value;
							vehicle.rudderCooldown = Byte.MAX_VALUE;
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
