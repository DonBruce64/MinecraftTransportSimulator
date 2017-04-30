package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.main.EntityPlane;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

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

	public static class Handler implements IMessageHandler<ElevatorPacket, IMessage> {
		public IMessage onMessage(ElevatorPacket message, MessageContext ctx) {
			EntityPlane thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if(message.packetType == 1){
					if(thisEntity.elevatorAngle + 6 <= 250){
						thisEntity.elevatorAngle += 6;
						thisEntity.elevatorCooldown = message.elevatorData;
					}else{
						return null;
					}
				}else if(message.packetType == -1){
					if(thisEntity.elevatorAngle - 6 >= -250){
						thisEntity.elevatorAngle -= 6;
						thisEntity.elevatorCooldown = message.elevatorData;
					}else{
						return null;
					}
				}else{
					thisEntity.elevatorAngle = message.elevatorData;
					thisEntity.elevatorCooldown = Short.MAX_VALUE;
				}
				if(ctx.side==Side.SERVER){
					MTS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}
