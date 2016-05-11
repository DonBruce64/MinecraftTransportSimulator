package minecraftflightsimulator.packets.control;

import net.minecraft.client.Minecraft;
import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityPlane;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class FlapPacket implements IMessage{	
	private int id;
	private byte flapAngle;	

	public FlapPacket() { }
	
	public FlapPacket(int id, byte flapAngle){
		this.id=id;
		this.flapAngle=flapAngle;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.flapAngle=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.flapAngle);
	}

	public static class FlapPacketHandler implements IMessageHandler<FlapPacket, IMessage> {
		public IMessage onMessage(FlapPacket message, MessageContext ctx) {
			EntityPlane thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if(ctx.side==Side.SERVER){
					if(message.flapAngle + thisEntity.flapAngle >= 0 && message.flapAngle + thisEntity.flapAngle <= 350){
						thisEntity.flapAngle += message.flapAngle;
						MFS.MFSNet.sendToAll(message);
					}
				}else{
					thisEntity.flapAngle += message.flapAngle;
				}
			}
			return null;
		}
	}
}