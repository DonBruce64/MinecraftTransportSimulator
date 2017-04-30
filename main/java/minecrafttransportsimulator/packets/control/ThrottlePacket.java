package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartVehicle;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class ThrottlePacket implements IMessage{
	private int id;
	private byte throttle;

	public ThrottlePacket() { }
	
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

	public static class Handler implements IMessageHandler<ThrottlePacket, IMessage> {
		public IMessage onMessage(ThrottlePacket message, MessageContext ctx) {
			EntityMultipartVehicle thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityMultipartVehicle) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityMultipartVehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if(message.throttle == Byte.MAX_VALUE){
					if(thisEntity.throttle < 100){
						++thisEntity.throttle;
					}
				}else if(message.throttle == Byte.MIN_VALUE){
					if(thisEntity.throttle > 0){
						--thisEntity.throttle;
					}
				}else{
					thisEntity.throttle = message.throttle;
				}
				if(ctx.side==Side.SERVER){
					MTS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}
