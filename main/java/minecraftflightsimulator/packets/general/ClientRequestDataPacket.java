package minecraftflightsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.entities.core.EntityBase;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class ClientRequestDataPacket implements IMessage{
	private int id;

	public ClientRequestDataPacket() { }
	
	public ClientRequestDataPacket(int id){
		this.id=id;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
	}

	public static class ClientRequestDataPacketHandler implements IMessageHandler<ClientRequestDataPacket, ServerSendDataPacket> {
		@Override
		public ServerSendDataPacket onMessage(ClientRequestDataPacket message, MessageContext ctx) {
			if(ctx.side==Side.SERVER){
				EntityBase thisEntity = (EntityBase) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
				if(thisEntity!=null){
					NBTTagCompound tagCompound = new NBTTagCompound();
					thisEntity.writeToNBT(tagCompound);
					return new ServerSendDataPacket(thisEntity.getEntityId(), tagCompound);
				}
			}
			return null;
		}
	}
}
