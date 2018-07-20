package minecrafttransportsimulator.packets.parts;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.multipart.main.EntityMultipartA_Base;
import minecrafttransportsimulator.multipart.parts.APart;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class APacketPart implements IMessage{
	private int id;
	private double x;
	private double y;
	private double z;

	public APacketPart(){}
	
	public APacketPart(APart part){
		this.id = part.multipart.getEntityId();
		this.x = part.offset.xCoord;
		this.y = part.offset.yCoord;
		this.z = part.offset.zCoord;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id = buf.readInt();
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeDouble(this.x);
		buf.writeDouble(this.y);
		buf.writeDouble(this.z);
	}
	
	protected static APart getMultipartPartFromMessage(APacketPart message, MessageContext ctx){
		EntityMultipartA_Base multipart;
		if(ctx.side.isServer()){
			multipart = (EntityMultipartA_Base) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
		}else{
			multipart = (EntityMultipartA_Base) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
		}
		if(multipart != null){
			for(APart part : multipart.getMultipartParts()){
				if(part.offset.xCoord == message.x && part.offset.yCoord == message.y && part.offset.zCoord == message.z){
					return part;
				}
			}
		}
		return null;
	}
}
