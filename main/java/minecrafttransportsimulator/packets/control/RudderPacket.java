package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RudderPacket implements IMessage{
	private int id;
	private byte packetType;
	private short rudderData;

	public RudderPacket() { }
	
	public RudderPacket(int id, boolean increment, short rudderCooldown){
		this.id=id;
		this.rudderData=rudderCooldown;
		this.packetType = (byte) (increment ? 1 : -1);
	}
	
	public RudderPacket(int id, short rudderAngle){
		this.id=id;
		this.rudderData=rudderAngle;
		this.packetType = 0;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.packetType=buf.readByte();
		this.rudderData=buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.packetType);
		buf.writeShort(this.rudderData);
	}

	public static class Handler implements IMessageHandler<RudderPacket, IMessage>{
		public IMessage onMessage(final RudderPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityPlane thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					}else{
						thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						if(message.packetType == 1){
							thisEntity.rudderCooldown = message.rudderData;
							if(thisEntity.rudderAngle + 6 <= 250){
								thisEntity.rudderAngle += 6;
							}else{
								return;
							}
						}else if(message.packetType == -1){
							thisEntity.rudderCooldown = message.rudderData;
							if(thisEntity.rudderAngle - 6 >= -250){
								thisEntity.rudderAngle -= 6;
							}else{
								return;
							}
						}else{
							thisEntity.rudderAngle = message.rudderData;
							thisEntity.rudderCooldown = Short.MAX_VALUE;
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
