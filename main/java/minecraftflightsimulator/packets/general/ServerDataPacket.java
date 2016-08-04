package minecraftflightsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecraftflightsimulator.entities.core.EntityBase;
import minecraftflightsimulator.entities.core.EntityParent;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

public class ServerDataPacket implements IMessage{
	private int id;
	private NBTTagCompound tagCompound;

	public ServerDataPacket() { }
	
	public ServerDataPacket(int id, NBTTagCompound tagCompound){
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

	public static class Handler implements IMessageHandler<ServerDataPacket, IMessage> {
		@Override
		public IMessage onMessage(ServerDataPacket message, MessageContext ctx) {
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
