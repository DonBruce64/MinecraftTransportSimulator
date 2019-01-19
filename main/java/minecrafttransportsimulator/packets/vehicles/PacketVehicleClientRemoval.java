package minecrafttransportsimulator.packets.vehicles;

import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketVehicleClientRemoval extends APacketVehicle{

	public PacketVehicleClientRemoval(){}
	
	public PacketVehicleClientRemoval(EntityVehicleE_Powered vehicle){
		super(vehicle);
	}

	public static class Handler implements IMessageHandler<PacketVehicleClientRemoval, IMessage>{
		@Override
		public IMessage onMessage(final PacketVehicleClientRemoval message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					EntityVehicleE_Powered vehicle = (EntityVehicleE_Powered) getVehicle(message, ctx);
					if(vehicle != null){
						SFXSystem.stopVehicleSounds(vehicle);
					}
				}
			});
			return null;
		}
	}
}
