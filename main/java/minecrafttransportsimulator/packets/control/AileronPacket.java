package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class AileronPacket implements IMessage{
	private int id;
	private byte packetType;
	private short aileronData;

	public AileronPacket() { }
	
	public AileronPacket(int id, boolean increment, short aileronCooldown){
		this.id=id;
		this.aileronData=aileronCooldown;
		this.packetType = (byte) (increment ? 1 : -1);
	}
	
	public AileronPacket(int id, short aileronAngle){
		this.id=id;
		this.aileronData=aileronAngle;
		this.packetType = 0;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.packetType=buf.readByte();
		this.aileronData=buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.packetType);
		buf.writeShort(this.aileronData);
	}

	public static class Handler implements IMessageHandler<AileronPacket, IMessage>{
		public IMessage onMessage(final AileronPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityPlane thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					}else{
						if(Minecraft.getMinecraft().theWorld == null){
							return;
						}
						thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						if(message.packetType == 1){
							thisEntity.aileronCooldown = message.aileronData;
							if(thisEntity.aileronAngle + 6 <= 250){
								thisEntity.aileronAngle += 6;
							}else{
								return;
							}
						}else if(message.packetType == -1){
							thisEntity.aileronCooldown = message.aileronData;
							if(thisEntity.aileronAngle - 6 >= -250){
								thisEntity.aileronAngle -= 6;
							}else{
								return;
							}
						}else{
							thisEntity.aileronAngle = message.aileronData;
							thisEntity.aileronCooldown = Short.MAX_VALUE;
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
