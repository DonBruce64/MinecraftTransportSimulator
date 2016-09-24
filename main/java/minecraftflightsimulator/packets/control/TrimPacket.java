package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.entities.core.EntityPlane;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

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

	public static class Handler implements IMessageHandler<TrimPacket, IMessage> {
		public IMessage onMessage(TrimPacket message, MessageContext ctx) {
			EntityPlane thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
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
				if(ctx.side==Side.SERVER){
					return message;
				}
			}
			return null;
		}
	}
}