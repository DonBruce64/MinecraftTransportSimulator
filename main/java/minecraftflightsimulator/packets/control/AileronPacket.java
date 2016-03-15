package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityPlane;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class AileronPacket implements IMessage{
	private int id;
	private byte aileronAngle;	

	public AileronPacket() { }
	
	public AileronPacket(int id, byte aileronAngle){
		this.id=id;
		this.aileronAngle=aileronAngle;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.aileronAngle=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.aileronAngle);
	}

	public static class AileronPacketHandler implements IMessageHandler<AileronPacket, IMessage> {
		public IMessage onMessage(AileronPacket message, MessageContext ctx) {
			EntityPlane thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if(ctx.side==Side.SERVER){
					if(message.aileronAngle + thisEntity.aileronAngle >= -250 && message.aileronAngle + thisEntity.aileronAngle <= 250){
						thisEntity.aileronAngle += message.aileronAngle;
						thisEntity.aileronCooldown = 4;
						MFS.MFSNet.sendToAll(message);
					}
				}else{
					thisEntity.aileronAngle += message.aileronAngle;
				}
			}
			return null;
		}
	}

}
