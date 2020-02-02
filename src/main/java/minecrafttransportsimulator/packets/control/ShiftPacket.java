package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Ground;
import minecrafttransportsimulator.vehicles.parts.APartEngineGeared;
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
					EntityVehicleF_Ground thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityVehicleF_Ground) ctx.getServerHandler().player.world.getEntityByID(message.id);
					}else{
						thisEntity = (EntityVehicleF_Ground) Minecraft.getMinecraft().world.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						APartEngineGeared<? extends EntityVehicleE_Powered> engine = (APartEngineGeared<? extends EntityVehicleE_Powered>) thisEntity.getEngineByNumber((byte) 0);
						if(engine != null){
							if(message.shiftUp){
								engine.shiftUp(true);
							}else{
								engine.shiftDown(true);
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