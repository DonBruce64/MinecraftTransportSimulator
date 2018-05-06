package minecrafttransportsimulator.packets.multipart;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketMultipart implements IMessage{
	protected int id;

	public APacketMultipart(){}
	
	public APacketMultipart(EntityMultipartA_Base multipart){
		this.id = multipart.getEntityId();
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
	}
	
	protected static EntityMultipartA_Base getMultipartFromMessage(APacketMultipart message, MessageContext ctx){
		if(ctx.side.isServer()){
			return (EntityMultipartA_Base) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
		}else{
			return (EntityMultipartA_Base) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
		}
	}
}
