package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.MTSEntity;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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
			if(ctx.side.isClient()){
				MTSEntity thisEntity = (MTSEntity) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
				if(thisEntity != null){
					thisEntity.readFromNBT(message.tagCompound);
					if(thisEntity instanceof EntityMultipartParent){
						((EntityMultipartParent) thisEntity).moveChildren();
					}
				}
			}
			return null;
		}
	}
}
