package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityPlane;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class RudderPacket implements IMessage{	
	private int id;
	private byte rudderAngle;	

	public RudderPacket() { }
	
	public RudderPacket(int id, byte rudderAngle){
		this.id=id;
		this.rudderAngle=rudderAngle;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.rudderAngle=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.rudderAngle);
	}

	public static class RudderPacketHandler implements IMessageHandler<RudderPacket, RudderPacket> {
		public RudderPacket onMessage(RudderPacket message, MessageContext ctx) {
			EntityPlane thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if(ctx.side==Side.SERVER){
					if(message.rudderAngle + thisEntity.rudderAngle >= -250 && message.rudderAngle + thisEntity.rudderAngle <= 250){
						thisEntity.rudderAngle += message.rudderAngle;
						thisEntity.rudderCooldown = MFS.controlSurfaceCooldown;
						MFS.MFSNet.sendToAll(message);
					}
				}else{
					thisEntity.rudderAngle += message.rudderAngle;
				}
			}
			return null;
		}
	}
}