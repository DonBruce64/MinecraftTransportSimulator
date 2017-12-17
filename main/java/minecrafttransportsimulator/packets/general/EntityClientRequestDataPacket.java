package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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

	public static class Handler implements IMessageHandler<EntityClientRequestDataPacket, IMessage>{
		@Override
		public IMessage onMessage(final EntityClientRequestDataPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					MTSEntity thisEntity = (MTSEntity) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					if(thisEntity!=null){
						NBTTagCompound tagCompound = new NBTTagCompound();
						thisEntity.writeToNBT(tagCompound);
						MTS.MTSNet.sendTo(new ServerDataPacket(thisEntity.getEntityId(), tagCompound), ctx.getServerHandler().playerEntity);
					}
				}
			});
			return null;
		}
	}
}
