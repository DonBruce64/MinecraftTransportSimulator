package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.EntityPlane;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

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

	public static class ThrottlePacketHandler implements IMessageHandler<ThrottlePacket, IMessage> {
		public IMessage onMessage(ThrottlePacket message, MessageContext ctx) {
			EntityPlane thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityPlane) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}
			if(thisEntity!=null){
				if(thisEntity.throttle < 100 && message.throttle == 111){
					++thisEntity.throttle;
				}else if(thisEntity.throttle > 15 && message.throttle == -111){
					--thisEntity.throttle;
				}else{
					thisEntity.throttle = message.throttle;
				}
				if(ctx.side==Side.SERVER){
					MFS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}
