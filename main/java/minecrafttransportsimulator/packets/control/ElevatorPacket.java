package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Plane;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ElevatorPacket implements IMessage{
	private int id;
	private byte packetType;
	private short elevatorData;

	public ElevatorPacket() { }
	
	public ElevatorPacket(int id, boolean increment, short elevatorCooldown){
		this.id=id;
		this.elevatorData=elevatorCooldown;
		this.packetType = (byte) (increment ? 1 : -1);
	}
	
	public ElevatorPacket(int id, short elevatorAngle){
		this.id=id;
		this.elevatorData=elevatorAngle;
		this.packetType = 0;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.packetType=buf.readByte();
		this.elevatorData=buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.packetType);
		buf.writeShort(this.elevatorData);
	}

	public static class Handler implements IMessageHandler<ElevatorPacket, IMessage>{
		public IMessage onMessage(final ElevatorPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleF_Plane thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityVehicleF_Plane) ctx.getServerHandler().player.world.getEntityByID(message.id);
					}else{
						if(Minecraft.getMinecraft().world == null){
							return;
						}
						thisEntity = (EntityVehicleF_Plane) Minecraft.getMinecraft().world.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						if(message.packetType == 1){
							thisEntity.elevatorCooldown = message.elevatorData;
							if(thisEntity.elevatorAngle + 6 <= 250){
								thisEntity.elevatorAngle += 6;
							}else{
								return;
							}
						}else if(message.packetType == -1){
							thisEntity.elevatorCooldown = message.elevatorData;
							if(thisEntity.elevatorAngle - 6 >= -250){
								thisEntity.elevatorAngle -= 6;
							}else{
								return;
							}
						}else{
							thisEntity.elevatorAngle = message.elevatorData;
							thisEntity.elevatorCooldown = Short.MAX_VALUE;
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
