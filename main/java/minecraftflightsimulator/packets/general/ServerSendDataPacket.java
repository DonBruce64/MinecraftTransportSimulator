package minecraftflightsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.entities.EntityBase;
import minecraftflightsimulator.entities.EntityParent;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class ServerSendDataPacket implements IMessage{
	private int id;
	private NBTTagCompound tagCompound;

	public ServerSendDataPacket() { }
	
	public ServerSendDataPacket(int id, NBTTagCompound tagCompound){
		this.id=id;
		this.tagCompound=tagCompound;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.tagCompound=ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		ByteBufUtils.writeTag(buf, this.tagCompound);
	}

	public static class ServerSendDataPacketHandler implements IMessageHandler<ServerSendDataPacket, IMessage> {
		@Override
		public IMessage onMessage(ServerSendDataPacket message, MessageContext ctx) {
			if(ctx.side==Side.CLIENT){
				EntityBase thisEntity = (EntityBase) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
				if(thisEntity != null){
					thisEntity.readFromNBT(message.tagCompound);
					if(thisEntity instanceof EntityParent){
						((EntityParent) thisEntity).moveChildren();
					}
				}
			}
			return null;
		}
	}
}
