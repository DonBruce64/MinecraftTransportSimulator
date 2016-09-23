package minecraftflightsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.MFS;
import minecraftflightsimulator.entities.core.EntityFlyable;
import net.minecraft.client.Minecraft;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class EnginePacket implements IMessage{
	private int id;
	private byte engineCode;
	private int engineID;

	public EnginePacket() { }
	
	public EnginePacket(int id, byte engineCode, int engineID){
		this.id=id;
		this.engineCode=engineCode;
		this.engineID=engineID;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.engineCode=buf.readByte();
		this.engineID=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.engineCode);
		buf.writeInt(this.engineID);
	}

	public static class Handler implements IMessageHandler<EnginePacket, IMessage> {
		public IMessage onMessage(EnginePacket message, MessageContext ctx){
			EntityFlyable thisEntity;
			if(ctx.side==Side.SERVER){
				thisEntity = (EntityFlyable) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
			}else{
				thisEntity = (EntityFlyable) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
			}	
			if(thisEntity!=null){
				if(thisEntity.setEngineState(message.engineCode, message.engineID) && ctx.side==Side.SERVER){
					MFS.MFSNet.sendToAll(message);
				}
			}
			return null;
		}
	}

}
