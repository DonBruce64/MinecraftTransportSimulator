package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.vehicles.main.EntityVehicleB_Existing;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleWindowBreak extends APacketVehicle{

	public PacketVehicleWindowBreak(){}
	
	public PacketVehicleWindowBreak(EntityVehicleB_Existing vehicle){
		super(vehicle);
	}

	public static class Handler implements IMessageHandler<PacketVehicleWindowBreak, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleWindowBreak message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleB_Existing vehicle = (EntityVehicleB_Existing) getVehicle(message, ctx);
					if(vehicle != null){
						++vehicle.brokenWindows;
					}	
				}
			});
			return null;
		}
	}

}
