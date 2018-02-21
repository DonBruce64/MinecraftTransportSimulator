package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.main.EntityCar;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SteeringPacket implements IMessage{
	private int id;
	private byte packetType;
	private short steeringData;

	public SteeringPacket() { }
	
	public SteeringPacket(int id, boolean increment, short steeringCooldown){
		this.id=id;
		this.steeringData=steeringCooldown;
		this.packetType = (byte) (increment ? 1 : -1);
	}
	
	public SteeringPacket(int id, short steeringAngle){
		this.id=id;
		this.steeringData=steeringAngle;
		this.packetType = 0;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.packetType=buf.readByte();
		this.steeringData=buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.packetType);
		buf.writeShort(this.steeringData);
	}

	public static class Handler implements IMessageHandler<SteeringPacket, IMessage>{
		public IMessage onMessage(final SteeringPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityCar thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityCar) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					}else{
						if(Minecraft.getMinecraft().theWorld == null){
							return;
						}
						thisEntity = (EntityCar) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						if(message.packetType == 1){
							thisEntity.steeringCooldown = message.steeringData;
							if(thisEntity.steeringAngle + 20 <= 250){
								thisEntity.steeringAngle += 20;
							}else{
								return;
							}
						}else if(message.packetType == -1){
							thisEntity.steeringCooldown = message.steeringData;
							if(thisEntity.steeringAngle - 20 >= -250){
								thisEntity.steeringAngle -= 20;
							}else{
								return;
							}
						}else{
							thisEntity.steeringAngle = message.steeringData;
							thisEntity.steeringCooldown = Short.MAX_VALUE;
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
