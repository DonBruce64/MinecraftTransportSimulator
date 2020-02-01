package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SteeringPacket implements IMessage{
	private int id;
	private short value;
	private byte cooldown;
	

	public SteeringPacket() { }
	
	public SteeringPacket(int id, short value, byte cooldown){
		this.id = id;
		this.value = value;
		this.cooldown = cooldown;
	}
	
	public SteeringPacket(int id, short steeringAngle){
		this.id = id;
		this.value = steeringAngle;
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

	public static class Handler implements IMessageHandler<SteeringPacket, IMessage>{
		public IMessage onMessage(final SteeringPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleF_Ground vehicle;
					if(ctx.side.isServer()){
						vehicle = (EntityVehicleF_Ground) ctx.getServerHandler().player.world.getEntityByID(message.id);
					}else{
						if(Minecraft.getMinecraft().world == null){
							return;
						}
						vehicle = (EntityVehicleF_Ground) Minecraft.getMinecraft().world.getEntityByID(message.id);
					}
					if(vehicle!=null){
						if(message.cooldown != -1){
							vehicle.steeringCooldown = message.cooldown;
							if(vehicle.steeringAngle + message.value > -vehicle.MAX_STEERING_ANGLE && vehicle.steeringAngle + message.value < vehicle.MAX_STEERING_ANGLE){
								vehicle.steeringAngle += message.value; 
							}else{
								return;
							}
						}else{
							vehicle.steeringAngle = message.value;
							vehicle.steeringCooldown = Byte.MAX_VALUE;
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
