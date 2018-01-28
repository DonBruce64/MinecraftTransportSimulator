package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MultipartDeltaPacket implements IMessage{
	private int id;
	private double deltaX;
	private double deltaY;
	private double deltaZ;
	private float deltaYaw;
	private float deltaPitch;
	private float deltaRoll;

	public MultipartDeltaPacket() { }
	
	public MultipartDeltaPacket(int id, double deltaX, double deltaY, double deltaZ, float deltaYaw, float deltaPitch, float deltaRoll){
		this.id=id;
		this.deltaX=deltaX;
		this.deltaY=deltaY;
		this.deltaZ=deltaZ;
		this.deltaYaw=deltaYaw;
		this.deltaPitch=deltaPitch;
		this.deltaRoll=deltaRoll;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.deltaX=buf.readDouble();
		this.deltaY=buf.readDouble();
		this.deltaZ=buf.readDouble();
		this.deltaYaw=buf.readFloat();
		this.deltaPitch=buf.readFloat();
		this.deltaRoll=buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeDouble(this.deltaX);
		buf.writeDouble(this.deltaY);
		buf.writeDouble(this.deltaZ);
		buf.writeFloat(this.deltaYaw);
		buf.writeFloat(this.deltaPitch);
		buf.writeFloat(this.deltaRoll);
	}

	public static class Handler implements IMessageHandler<MultipartDeltaPacket, IMessage>{
		@Override
		public IMessage onMessage(final MultipartDeltaPacket message, final MessageContext ctx){
			
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartMoving thisEntity = (EntityMultipartMoving) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					if(thisEntity != null){
						thisEntity.addToServerDeltas(message.deltaX, message.deltaY, message.deltaZ, message.deltaYaw, message.deltaPitch, message.deltaRoll);
					}
				}
			});
			return null;
		}
	}
}
