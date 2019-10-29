package minecrafttransportsimulator.packets.control;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Air;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class ReverseThrustPacket implements IMessage{
	private int id;
	private boolean reversed;

	public ReverseThrustPacket(){}
	
	public ReverseThrustPacket(int id, boolean reversed){
		this.id = id;
		this.reversed = reversed;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.id=buf.readInt();
		this.reversed=buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeInt(this.id);
		buf.writeBoolean(this.reversed);
	}

	public static class Handler implements IMessageHandler<ReverseThrustPacket, IMessage>{
		public IMessage onMessage(final ReverseThrustPacket message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleF_Air thisEntity;
					if(ctx.side.isServer()){
						thisEntity = (EntityVehicleF_Air) ctx.getServerHandler().player.world.getEntityByID(message.id);
					}else{
						thisEntity = (EntityVehicleF_Air) Minecraft.getMinecraft().world.getEntityByID(message.id);
					}
					if(thisEntity!=null){
						thisEntity.reverseThrust = message.reversed;
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
