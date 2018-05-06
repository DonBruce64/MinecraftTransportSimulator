package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityMultipartMoving;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class BrakePacket implements IMessage{
	private int id;
	private byte brakeCode;

	public BrakePacket() { }
	
	public BrakePacket(int id, byte brakeCode){
		this.id=id;
		this.brakeCode=brakeCode;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.brakeCode=buf.readByte();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeByte(this.brakeCode);
	}

	public static class Handler implements IMessageHandler<BrakePacket, IMessage>{
		public IMessage onMessage(final BrakePacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartMoving thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityMultipartMoving) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					}else{
						thisEntity = (EntityMultipartMoving) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						if((message.brakeCode & 2) == 2){
							thisEntity.brakeOn = (message.brakeCode & 1) == 1 ? true : false;
						}
						if((message.brakeCode & 8) == 8){
							boolean wasParkingBrakeOn = thisEntity.parkingBrakeOn;
							thisEntity.parkingBrakeOn = (message.brakeCode & 4) == 4 ? true : false;
							if(thisEntity.parkingBrakeOn && !wasParkingBrakeOn && thisEntity instanceof EntityMultipartF_Car && thisEntity.pack != null && thisEntity.pack.car.isBigTruck){
								MTS.proxy.playSound(thisEntity, MTS.MODID + ":air_brake_activating", 1.0F, 1);
							}
						}
						if(ctx.side.isServer()){
							MTS.MTSNet.sendToAll(message);
						}
					}
				}
			});
			return null;
		}
	}

}
