package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.parts.EntityEngineCar;
import minecrafttransportsimulator.multipart.main.EntityMultipartF_Car;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ShiftPacket implements IMessage{	
	private int id;
	private boolean shiftUp;	

	public ShiftPacket() { }
	
	public ShiftPacket(int id, boolean shiftUp){
		this.id=id;
		this.shiftUp=shiftUp;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.shiftUp=buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeBoolean(this.shiftUp);
	}

	public static class Handler implements IMessageHandler<ShiftPacket, IMessage>{
		public IMessage onMessage(final ShiftPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityMultipartF_Car thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityMultipartF_Car) ctx.getServerHandler().playerEntity.worldObj.getEntityByID(message.id);
					}else{
						thisEntity = (EntityMultipartF_Car) Minecraft.getMinecraft().theWorld.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						EntityEngineCar carEngine = (EntityEngineCar) thisEntity.getEngineByNumber((byte) 1);
						if(carEngine != null){
							if(message.shiftUp){
								if(carEngine.isAutomatic ? carEngine.getCurrentGear() < 1 : true){
									carEngine.shiftUp();
								}
							}else{
								if(carEngine.isAutomatic ? carEngine.getCurrentGear() > -1 : true){
									carEngine.shiftDown();
								}
							}
							if(ctx.side.isServer()){
								MTS.MTSNet.sendToAll(message);
							}
						}
					}
				}
			});
			return null;
		}
	}
}