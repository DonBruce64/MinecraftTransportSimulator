package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityParent;
import minecraftflightsimulator.entities.core.EntityPlane;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class EnginePacket implements IMessage{
	private int id;
	private byte engineCode;

	public EnginePacket() { }
	
	public EnginePacket(int id, byte engineCode){
		this.id=id;
		this.engineCode=engineCode;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.engineCode=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.engineCode);
	}

	public static class EnginePacketHandler implements IMessageHandler<EnginePacket, IMessage> {
		public IMessage onMessage(EnginePacket message, MessageContext ctx){
			EntityParent thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityParent) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityPlane) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}	
			if(thisEntity!=null){
				thisEntity.setEngineState(message.engineCode);
				if(ctx.side==Side.SERVER){
					MFS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}
