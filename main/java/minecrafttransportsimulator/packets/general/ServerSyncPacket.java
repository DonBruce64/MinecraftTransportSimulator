package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ServerSyncPacket implements IMessage{
	private int id;
	private double posX;
	private double posY;
	private double posZ;
	private double motionX;
	private double motionY;
	private double motionZ;
	private float pitch;
	private float roll;
	private float yaw;

	public ServerSyncPacket() { }
	
	public ServerSyncPacket(int id, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float pitch, float roll){
		this.id=id;
		this.posX=posX;
		this.posY=posY;
		this.posZ=posZ;
		this.motionX=motionX;
		this.motionY=motionY;
		this.motionZ=motionZ;
		this.pitch=pitch;
		this.roll=roll;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.posX=buf.readDouble();
		this.posY=buf.readDouble();
		this.posZ=buf.readDouble();
		this.motionX=buf.readDouble();
		this.motionY=buf.readDouble();
		this.motionZ=buf.readDouble();
		this.pitch=buf.readFloat();
		this.roll=buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeDouble(this.posX);
		buf.writeDouble(this.posY);
		buf.writeDouble(this.posZ);
		buf.writeDouble(this.motionX);
		buf.writeDouble(this.motionY);
		buf.writeDouble(this.motionZ);
		buf.writeFloat(this.pitch);
		buf.writeFloat(this.roll);
	}

	public static class Handler implements IMessageHandler<ServerSyncPacket, IMessage>{
		
		@Override
		public IMessage onMessage(final ServerSyncPacket message, final MessageContext ctx){
			
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new RunnableSync(){
				@Override
				public void run(){
					EntityMultipartParent thisEntity = (EntityMultipartParent) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					if(thisEntity != null){
						forcedSync = false;
						byte syncThreshold = (byte) ConfigSystem.getIntegerConfig("SyncThreshold");
						float syncIncrement = (float) ConfigSystem.getDoubleConfig("IncrementalMovement");
						thisEntity.posX = rectifyValue(thisEntity.posX, message.posX, syncIncrement, syncThreshold);
						thisEntity.posY = rectifyValue(thisEntity.posY, message.posY, syncIncrement, syncThreshold);
						thisEntity.posZ = rectifyValue(thisEntity.posZ, message.posZ, syncIncrement, syncThreshold);
						
						thisEntity.motionX = rectifyValue(thisEntity.motionX, message.motionX, syncIncrement, syncThreshold/25F);
						thisEntity.motionY = rectifyValue(thisEntity.motionY, message.motionY, syncIncrement, syncThreshold/25F);
						thisEntity.motionZ = rectifyValue(thisEntity.motionZ, message.motionZ, syncIncrement, syncThreshold/25F);
						
						thisEntity.rollCorrection = thisEntity.rotationRoll;
						thisEntity.rotationRoll = (float) rectifyValue(thisEntity.rotationRoll, message.roll, syncIncrement, syncThreshold);
						thisEntity.rollCorrection -= thisEntity.rotationRoll;
						
						thisEntity.pitchCorrection = thisEntity.rotationPitch;
						thisEntity.rotationPitch = (float) rectifyValue(thisEntity.rotationPitch, message.pitch, syncIncrement, syncThreshold);
						thisEntity.pitchCorrection -= thisEntity.rotationPitch; 
						
						thisEntity.moveChildren();
						if(forcedSync){
							thisEntity.requestDataFromServer();
						}
					}
				}
			});
			return null;
		}
		
		
		
		private abstract static class RunnableSync implements Runnable{
			protected static boolean forcedSync;
			
			protected static double rectifyValue(double currentValue, double packetValue, double increment, double cutoff){
				if(currentValue > packetValue){
					if(currentValue - packetValue > cutoff){
						forcedSync = true;
						return packetValue;
					}else{
						return currentValue - Math.min(currentValue - packetValue, increment);
					}
				}else{
					if(packetValue - currentValue > cutoff){
						forcedSync = true;
						return packetValue;
					}else{
						return currentValue + Math.min(packetValue - currentValue, increment);
					}
				}
			}
		}
	}
}
