package minecrafttransportsimulator.packets.general;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.dataclasses.MTSEntity;
import net.minecraft.nbt.NBTTagCompound;

public class EntityClientRequestDataPacket implements IMessage{
	private int id;

	public EntityClientRequestDataPacket() { }
	
	public EntityClientRequestDataPacket(int id){
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

	public static class Handler implements IMessageHandler<EntityClientRequestDataPacket, ServerDataPacket> {
		@Override
		public ServerDataPacket onMessage(EntityClientRequestDataPacket message, MessageContext ctx) {
			if(ctx.side.isServer()){
				MTSEntity thisEntity = (MTSEntity) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
				if(thisEntity!=null){
					NBTTagCompound tagCompound = new NBTTagCompound();
					thisEntity.writeToNBT(tagCompound);
					return new ServerDataPacket(thisEntity.getEntityId(), tagCompound);
				}
			}
			return null;
		}
	}
}
