package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleNameTag extends APacketVehicle{
	private String displayText; 

	public PacketVehicleNameTag(){}
	
	public PacketVehicleNameTag(EntityVehicleF_Physics vehicle){
		super(vehicle);
		this.displayText = vehicle.displayText;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.displayText = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, this.displayText);
	}

	public static class Handler implements IMessageHandler<PacketVehicleNameTag, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleNameTag message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleF_Physics vehicle = getVehicle(message, ctx);
					if(vehicle != null){
						vehicle.displayText = message.displayText;
					}
				}
			});
			return null;
		}
	}
}
