package minecrafttransportsimulator.packets.vehicles;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleJerrycan extends APacketVehicle{
	private String fluidName;

	public PacketVehicleJerrycan(){}
	
	public PacketVehicleJerrycan(EntityVehicleB_Existing vehicle, String fluidName){
		super(vehicle);
		this.fluidName = fluidName;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		super.fromBytes(buf);
		this.fluidName = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf){
		super.toBytes(buf);
		ByteBufUtils.writeUTF8String(buf, this.fluidName);
	}

	public static class Handler implements IMessageHandler<PacketVehicleJerrycan, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleJerrycan message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) getVehicle(message, ctx);
					if(vehicle != null){
						vehicle.fluidName = message.fluidName;
						vehicle.fuel += 1000;
					}
				}
			});
			return null;
		}
	}
}
