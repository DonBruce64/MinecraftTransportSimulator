package minecrafttransportsimulator.packets.control;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityVehicle;
import net.minecraft.client.Minecraft;

public class LightPacket implements IMessage{
	private int id;
	private byte lightCode;

	public LightPacket() { }
	
	public LightPacket(int id, byte lightCode){
		this.id=id;
		this.lightCode=lightCode;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.lightCode=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.lightCode);
	}

	public static class Handler implements IMessageHandler<LightPacket, IMessage> {
		public IMessage onMessage(LightPacket message, MessageContext ctx){
			EntityVehicle thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityVehicle) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityVehicle) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				//Toggle the light in the specified slot.
				byte shift = 0;
				while(message.lightCode>>shift != 1){
					++shift;
				}
				if((thisEntity.lightStatus>>shift & 1) == 1){
					thisEntity.lightStatus = (byte) (thisEntity.lightStatus ^ message.lightCode);
				}else{
					thisEntity.lightStatus = (byte) (thisEntity.lightStatus | message.lightCode);
				}
				if(ctx.side==Side.SERVER){
					MTS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}
